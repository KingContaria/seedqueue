package me.contaria.seedqueue.gui.wall;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.SeedQueueSettingsCache;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.keybindings.SeedQueueKeyBindings;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.contaria.seedqueue.sounds.SeedQueueSounds;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SeedQueueWallScreen extends Screen {

    private static final Set<WorldRenderer> WORLD_RENDERERS = new HashSet<>();

    private static final Identifier WALL_BACKGROUND = new Identifier("seedqueue", "textures/gui/wall/background.png");
    private static final Identifier WALL_OVERLAY = new Identifier("seedqueue", "textures/gui/wall/overlay.png");
    private static final Identifier INSTANCE_BACKGROUND = new Identifier("seedqueue", "textures/gui/wall/instance_background.png");

    private final Screen createWorldScreen;

    protected final SeedQueueSettingsCache settingsCache;
    private SeedQueueSettingsCache lastSettingsCache;

    private Layout layout;
    private SeedQueuePreview[] mainLoadingScreens;
    @Nullable
    private List<SeedQueuePreview> lockedLoadingScreens;
    private List<SeedQueuePreview> preparingLoadingScreens;

    private List<LockTexture> lockTextures;

    protected int frame;
    private int nextSoundFrame;

    private final long benchmarkStart = System.currentTimeMillis();
    private int benchmarkedSeeds;

    public SeedQueueWallScreen(Screen createWorldScreen) {
        super(LiteralText.EMPTY);
        this.createWorldScreen = createWorldScreen;
        this.preparingLoadingScreens = new ArrayList<>(SeedQueue.config.backgroundPreviews);
        this.lastSettingsCache = this.settingsCache = SeedQueueSettingsCache.create();
    }

    @Override
    protected void init() {
        assert this.client != null;
        this.layout = this.createLayout();
        this.mainLoadingScreens = new SeedQueuePreview[this.layout.main.size()];
        this.lockedLoadingScreens = this.layout.locked != null ? new ArrayList<>() : null;
        this.preparingLoadingScreens = new ArrayList<>();
        this.lockTextures = this.createLockTextures();
    }

    private Layout createLayout() {
        assert this.client != null;
        if (SeedQueue.config.customLayout != null) {
            try {
                return Layout.fromJson(SeedQueue.config.customLayout);
            } catch (Exception e) {
                SeedQueue.LOGGER.warn("Failed to parse custom wall layout!", e);
            }
        }
        return Layout.grid(SeedQueue.config.rows, SeedQueue.config.columns, this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
    }

    private List<LockTexture> createLockTextures() {
        assert this.client != null;
        List<LockTexture> lockTextures = new ArrayList<>();
        Identifier lock;
        while (this.client.getResourceManager().containsResource(lock = new Identifier("seedqueue", "textures/gui/wall/lock-" + lockTextures.size() + ".png"))) {
            try {
                lockTextures.add(new LockTexture(lock));
            } catch (IOException e) {
                SeedQueue.LOGGER.warn("Failed to read lock image texture: {}", lock, e);
            }
        }
        return lockTextures;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        this.frame++;
        this.updatePreviews();
        if (this.client.getResourceManager().containsResource(WALL_BACKGROUND)) {
            this.client.getTextureManager().bindTexture(WALL_BACKGROUND);
            Screen.drawTexture(matrices, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        } else {
            this.renderBackground(matrices);
        }

        for (int i = 0; i < this.layout.main.size(); i++) {
            this.renderInstance(this.mainLoadingScreens[i], this.layout.main, this.layout.main.getPos(i), matrices, delta);
        }
        if (this.layout.locked != null && this.lockedLoadingScreens != null) {
            for (int i = 0; i < this.layout.locked.size(); i++) {
                this.renderInstance(i < this.lockedLoadingScreens.size() ? this.lockedLoadingScreens.get(i) : null, this.layout.locked, this.layout.locked.getPos(i), matrices, delta);
            }
        }
        int i = 0;
        for (Layout.Group group : this.layout.more) {
            int offset = i;
            for (i = 0; i < group.size(); i++) {
                this.renderInstance(i < this.preparingLoadingScreens.size() ? this.preparingLoadingScreens.get(i) : null, group, group.getPos(i - offset), matrices, delta);
            }
        }

        if (this.client.getResourceManager().containsResource(WALL_OVERLAY)) {
            this.client.getTextureManager().bindTexture(WALL_OVERLAY);
            Screen.drawTexture(matrices, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        }

        for (SeedQueuePreview preparingInstance : this.preparingLoadingScreens) {
            if (preparingInstance.hasBeenRendered()) {
                continue;
            }
            this.loadPreviewSettings(preparingInstance);
            preparingInstance.buildChunks();
        }
        this.resetViewport();

        this.loadPreviewSettings(this.settingsCache, 0);
    }

    private void renderInstance(SeedQueuePreview instance, Layout.Group group, Layout.Pos pos, MatrixStack matrices, float delta) {
        assert this.client != null;
        if (pos == null) {
            return;
        }
        try {
            RenderSystem.viewport(pos.x, this.client.getWindow().getFramebufferHeight() - pos.height - pos.y, pos.width, pos.height);
            if (instance == null || !instance.shouldRender()) {
                if (group.instance_background && this.client.getResourceManager().containsResource(INSTANCE_BACKGROUND)) {
                    this.client.getTextureManager().bindTexture(INSTANCE_BACKGROUND);
                    Screen.drawTexture(matrices, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
                }
                return;
            }
            this.loadPreviewSettings(instance);
            instance.render(matrices, 0, 0, delta);
        } finally {
            this.resetViewport();
        }
        if (instance.getSeedQueueEntry().isLocked() && !this.lockTextures.isEmpty()) {
            if (instance.lock == null) {
                instance.lock = this.lockTextures.get(new Random().nextInt(this.lockTextures.size()));
            }
            this.client.getTextureManager().bindTexture(instance.lock.id);
            double scale = this.client.getWindow().getScaleFactor();
            Screen.drawTexture(matrices, (int) (pos.x / scale), (int) (pos.y / scale), 0.0f, 0.0f, (int) (pos.height * instance.lock.aspectRatio / scale), (int) (pos.height / scale), (int) (pos.height * instance.lock.aspectRatio / scale), (int) (pos.height / scale));
        }
    }

    @SuppressWarnings("deprecation")
    private void resetViewport() {
        assert this.client != null;
        Window window = this.client.getWindow();
        RenderSystem.viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
        // see GameRenderer#render or WorldPreview#render
        // we need this to reset RenderSystem.ortho after simulating a different window size
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, (double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
    }

    private void updatePreviews() {
        if (this.lockedLoadingScreens != null) {
            for (int i = 0; i < this.mainLoadingScreens.length; i++) {
                SeedQueuePreview instance = this.mainLoadingScreens[i];
                if (instance != null && instance.getSeedQueueEntry().isLocked()) {
                    this.lockedLoadingScreens.add(instance);
                    this.mainLoadingScreens[i] = null;
                }
            }
            for (SeedQueuePreview instance : this.preparingLoadingScreens) {
                if (instance.getSeedQueueEntry().isLocked()) {
                    this.lockedLoadingScreens.add(instance);
                }
            }
            this.preparingLoadingScreens.removeAll(this.lockedLoadingScreens);
        }

        this.preparingLoadingScreens.sort(Comparator.comparing(SeedQueuePreview::shouldRender, Comparator.reverseOrder()));

        for (int i = 0; i < this.mainLoadingScreens.length && !this.preparingLoadingScreens.isEmpty() && this.preparingLoadingScreens.get(0).shouldRender(); i++) {
            if (this.mainLoadingScreens[i] == null && !this.blockedMainPositions.contains(i)) {
                this.preparingLoadingScreens.remove(this.mainLoadingScreens[i] = this.preparingLoadingScreens.remove(0));
            }
        }

        int urgent = (int) Arrays.stream(this.mainLoadingScreens).filter(Objects::isNull).count();
        int capacity = SeedQueue.config.backgroundPreviews + urgent;
        if (this.preparingLoadingScreens.size() < capacity) {
            int budget = Math.max(1, urgent);
            for (SeedQueueEntry entry : this.getAvailableSeedQueueEntries()) {
                this.preparingLoadingScreens.add(new SeedQueuePreview(this, entry));
                if (--budget <= 0) {
                    break;
                }
            }
        } else {
            clearWorldRenderer(getClearableWorldRenderer());
        }
    }

    private List<SeedQueueEntry> getAvailableSeedQueueEntries() {
        List<SeedQueueEntry> entries = new ArrayList<>(SeedQueue.SEED_QUEUE);
        entries.removeAll(this.getInstances().stream().map(SeedQueuePreview::getSeedQueueEntry).collect(Collectors.toList()));
        entries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldGenerationProgressTracker() == null);
        entries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldPreviewProperties() == null);
        if (this.lockedLoadingScreens != null) {
            entries.sort(Comparator.comparing(SeedQueueEntry::isLocked, Comparator.reverseOrder()));
        }
        return entries;
    }

    private void loadPreviewSettings(SeedQueuePreview instance) {
        this.loadPreviewSettings(instance.getWorldPreviewProperties().getSettingsCache(), instance.getWorldPreviewProperties().getPerspective());
    }

    private void loadPreviewSettings(SeedQueueSettingsCache settingsCache, int perspective) {
        assert this.client != null;
        if (settingsCache != this.lastSettingsCache) {
            settingsCache.loadPreview();
            this.lastSettingsCache = settingsCache;
        }
        this.client.options.perspective = perspective;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        assert this.client != null;

        if (this.isBenchmarking()) {
            return true;
        }

        if (SeedQueueKeyBindings.resetAll.matchesMouse(button)) {
            this.resetAllInstances();
        }

        if (SeedQueueKeyBindings.resetColumn.matchesMouse(button)) {
            this.resetColumn(mouseX);
        }

        if (SeedQueueKeyBindings.resetRow.matchesMouse(button)) {
            this.resetRow(mouseY);
        }

        if (SeedQueueKeyBindings.playNextLock.matchesMouse(button)) {
            SeedQueue.getEntryMatching(SeedQueueEntry::isLocked).ifPresent(this::playInstance);
        }

        SeedQueuePreview instance = this.getInstance(mouseX, mouseY);
        if (instance == null) {
            return true;
        }

        if (SeedQueueKeyBindings.play.matchesMouse(button)) {
            this.playInstance(instance);
        }
        if (SeedQueueKeyBindings.lock.matchesMouse(button)) {
            this.lockInstance(instance);
        }
        if (SeedQueueKeyBindings.reset.matchesMouse(button)) {
            this.resetInstance(instance, true);
        }
        if (SeedQueueKeyBindings.focusReset.matchesMouse(button)) {
            if (instance.getSeedQueueEntry().isReady()) {
                this.playInstance(instance);
            } else {
                this.lockInstance(instance);
            }
            this.resetAllInstances();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        assert this.client != null;
        Window window = this.client.getWindow();
        double mouseX = this.client.mouse.getX() * window.getScaledWidth() / window.getWidth();
        double mouseY = this.client.mouse.getY() * window.getScaledWidth() / window.getWidth();

        if (this.isBenchmarking()) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && Screen.hasShiftDown()) {
            ModCompat.standardsettings$onWorldJoin();
            Atum.stopRunning();
            this.client.openScreen(new TitleScreen());
            return true;
        }

        if (SeedQueueKeyBindings.resetAll.matchesKey(keyCode, scanCode)) {
            this.resetAllInstances();
        }

        if (SeedQueueKeyBindings.playNextLock.matchesKey(keyCode, scanCode)) {
            SeedQueue.getEntryMatching(SeedQueueEntry::isLocked).ifPresent(this::playInstance);
        }

        if (SeedQueueKeyBindings.resetColumn.matchesKey(keyCode, scanCode)) {
            this.resetColumn(mouseX);
        }

        if (SeedQueueKeyBindings.resetRow.matchesKey(keyCode, scanCode)) {
            this.resetRow(mouseY);
        }

        SeedQueuePreview instance = this.getInstance(mouseX, mouseY);
        if (instance == null) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F3) {
            instance.printDebug();
            if (Screen.hasShiftDown()) {
                instance.printStacktrace();
            }
        }

        if (SeedQueueKeyBindings.play.matchesKey(keyCode, scanCode)) {
            this.playInstance(instance);
        }
        if (SeedQueueKeyBindings.lock.matchesKey(keyCode, scanCode)) {
            this.lockInstance(instance);
        }
        if (SeedQueueKeyBindings.reset.matchesKey(keyCode, scanCode)) {
            this.resetInstance(instance, true);
        }
        if (SeedQueueKeyBindings.focusReset.matchesKey(keyCode, scanCode)) {
            if (instance.getSeedQueueEntry().isReady()) {
                this.playInstance(instance);
            } else {
                this.lockInstance(instance);
            }
            this.resetAllInstances();
        }
        return true;
    }

    private SeedQueuePreview getInstance(double mouseX, double mouseY) {
        assert this.client != null;
        double scale = this.client.getWindow().getScaleFactor();
        double x = mouseX * scale;
        double y = mouseY * scale;

        // we traverse the layout in reverse to catch the top rendered instance
        for (int i = this.layout.more.length - 1; i >= 0; i--) {
            Optional<SeedQueuePreview> instance = this.getInstance(this.layout.more[i], x, y).filter(index -> index < this.preparingLoadingScreens.size()).map(this.preparingLoadingScreens::get);
            if (instance.isPresent()) {
                return instance.get();
            }
        }
        if (this.layout.locked != null && this.lockedLoadingScreens != null) {
            Optional<SeedQueuePreview> instance = this.getInstance(this.layout.locked, x, y).filter(index -> index < this.lockedLoadingScreens.size()).map(this.lockedLoadingScreens::get);
            if (instance.isPresent()) {
                return instance.get();
            }
        }
        return this.getInstance(this.layout.main, x, y).map(index -> this.mainLoadingScreens[index]).orElse(null);
    }

    private Optional<Integer> getInstance(Layout.Group group, double mouseX, double mouseY) {
        if (group.cosmetic) {
            return Optional.empty();
        }
        for (int i = group.size() - 1; i >= 0; i--) {
            Layout.Pos pos = group.getPos(i);
            if (mouseX >= pos.x && mouseX <= pos.x + pos.width && mouseY >= pos.y && mouseY <= pos.y + pos.height) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private List<SeedQueuePreview> getInstances() {
        List<SeedQueuePreview> instances = new ArrayList<>();
        for (SeedQueuePreview instance : this.mainLoadingScreens) {
            if (instance != null) {
                instances.add(instance);
            }
        }
        instances.addAll(this.preparingLoadingScreens);
        if (this.lockedLoadingScreens != null) {
            instances.addAll(this.lockedLoadingScreens);
        }
        return instances;
    }

    private void playInstance(SeedQueuePreview instance) {
        if (instance.hasBeenRendered()) {
            this.playInstance(instance.getSeedQueueEntry());
        }
    }

    private void playInstance(SeedQueueEntry entry) {
        assert this.client != null;
        if (!entry.isReady() || SeedQueue.selectedEntry != null) {
            return;
        }
        SeedQueue.selectedEntry = entry;
        this.client.openScreen(this.createWorldScreen);
    }

    private void lockInstance(SeedQueuePreview instance) {
        if (instance.getSeedQueueEntry().lock()) {
            this.playSound(SeedQueueSounds.LOCK_INSTANCE);
        }
    }

    private void resetInstance(SeedQueuePreview instance) {
        this.resetInstance(instance, false);
    }

    private boolean resetInstance(SeedQueuePreview instance, boolean ignoreLock) {
        if (instance == null) {
            return false;
        }
        SeedQueueEntry seedQueueEntry = instance.getSeedQueueEntry();
        if (!instance.hasBeenRendered() || (seedQueueEntry.isLocked() && !ignoreLock) || SeedQueue.selectedEntry == seedQueueEntry) {
            return false;
        }

        if (SeedQueue.remove(seedQueueEntry)) {
            seedQueueEntry.discard();
        }

        for (int i = 0; i < this.mainLoadingScreens.length; i++) {
            if (this.mainLoadingScreens[i] == instance) {
                this.mainLoadingScreens[i] = null;
            }
            this.preparingLoadingScreens.remove(instance);
            if (this.lockedLoadingScreens != null) {
                this.lockedLoadingScreens.remove(instance);
            }
        }
        this.playSound(SeedQueueSounds.RESET_INSTANCE);
        return true;
    }

    private void resetAllInstances() {
        for (SeedQueuePreview instance : this.mainLoadingScreens) {
            this.resetInstance(instance);
        }
    }

    private void resetColumn(double mouseX) {
        assert this.client != null;
        double x = mouseX * this.client.getWindow().getScaleFactor();
        for (int i = 0; i < this.mainLoadingScreens.length; i++) {
            Layout.Pos pos = this.layout.main.getPos(i);
            if (x >= pos.x && x <= pos.x + pos.width) {
                this.resetInstance(this.mainLoadingScreens[i]);
            }
        }
    }

    private void resetRow(double mouseY) {
        assert this.client != null;
        double y = mouseY * this.client.getWindow().getScaleFactor();
        for (int i = 0; i < this.mainLoadingScreens.length; i++) {
            Layout.Pos pos = this.layout.main.getPos(i);
            if (y >= pos.y && y <= pos.y + pos.height) {
                this.resetInstance(this.mainLoadingScreens[i]);
            }
        }
    }

    private void playSound(SoundEvent sound) {
        if (this.isBenchmarking()) {
            return;
        }

        assert this.client != null;
        if (this.nextSoundFrame < this.frame) {
            this.client.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0f));
            this.nextSoundFrame = this.frame;
        } else {
            this.client.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0f), ++this.nextSoundFrame - this.frame);
        }
    }

    public void tickBenchmark() {
        assert this.client != null;
        if (!this.isBenchmarking()) {
            return;
        }
        for (SeedQueuePreview instance : this.mainLoadingScreens) {
            if (this.resetInstance(instance, true)) {
                this.benchmarkedSeeds++;
                if (this.benchmarkedSeeds == SeedQueue.config.benchmarkResets) {
                    double time = Math.round((System.currentTimeMillis() - this.benchmarkStart) / 10.0) / 100.0;
                    this.client.getToastManager().add(new SystemToast(SystemToast.Type.WORLD_BACKUP, new TranslatableText("seedqueue.menu.benchmark"), new TranslatableText("seedqueue.menu.benchmark.result", this.benchmarkedSeeds, time)));
                    SeedQueue.LOGGER.info("BENCHMARK | Reset {} seeds in {} seconds.", this.benchmarkedSeeds, time);
                    break;
                }
            }
        }
    }

    private boolean isBenchmarking() {
        return this.benchmarkedSeeds < SeedQueue.config.benchmarkResets;
    }

    public static WorldRenderer getOrCreateWorldRenderer(ClientWorld world) {
        WorldRenderer worldRenderer = getWorldRenderer(world);
        if (worldRenderer != null) {
            return worldRenderer;
        }
        worldRenderer = getClearableWorldRenderer();
        if (worldRenderer != null) {
            worldRenderer.setWorld(world);
            return worldRenderer;
        }
        worldRenderer = getClearedWorldRenderer();
        if (worldRenderer != null) {
            worldRenderer.setWorld(world);
            return worldRenderer;
        }
        worldRenderer = new WorldRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
        WORLD_RENDERERS.add(worldRenderer);
        worldRenderer.setWorld(world);
        return worldRenderer;
    }

    public static void clearWorldRenderers() {
        for (WorldRenderer worldRenderer : WORLD_RENDERERS) {
            worldRenderer.setWorld(null);
            worldRenderer.close();
        }
        WORLD_RENDERERS.clear();
    }

    private static WorldRenderer getWorldRenderer(ClientWorld world) {
        for (WorldRenderer worldRenderer : WORLD_RENDERERS) {
            if (getWorld(worldRenderer) == world) {
                return worldRenderer;
            }
        }
        return null;
    }

    private static WorldRenderer getClearableWorldRenderer() {
        for (WorldRenderer worldRenderer : WORLD_RENDERERS) {
            ClientWorld worldRendererWorld = getWorld(worldRenderer);
            if (!SeedQueue.getEntryMatching(entry -> {
                WorldPreviewProperties wpProperties = entry.getWorldPreviewProperties();
                return wpProperties != null && wpProperties.getWorld() == worldRendererWorld;
            }).isPresent()) {
                return worldRenderer;
            }
        }
        return null;
    }

    private static WorldRenderer getClearedWorldRenderer() {
        return getWorldRenderer(null);
    }

    private static void clearWorldRenderer(WorldRenderer worldRenderer) {
        if (worldRenderer != null) {
            worldRenderer.setWorld(null);
        }
    }

    private static ClientWorld getWorld(WorldRenderer worldRenderer) {
        return ((WorldRendererAccessor) worldRenderer).seedQueue$getWorld();
    }

    public static class LockTexture {
        private final Identifier id;
        private final double aspectRatio;

        public LockTexture(Identifier id) throws IOException {
            this.id = id;
            try (NativeImage image = NativeImage.read(MinecraftClient.getInstance().getResourceManager().getResource(id).getInputStream())) {
                this.aspectRatio = (double) image.getWidth() / image.getHeight();
            }
        }
    }

    public static class Layout {
        @NotNull
        private final Group main;
        @Nullable
        private final Group locked;
        private final Group[] more;

        public Layout(@NotNull Group main) {
            this(main, null, new Group[0]);
        }

        public Layout(@NotNull Group main, @Nullable Group locked, Group[] more) {
            this.main = main;
            this.locked = locked;
            this.more = more;

            if (this.main.cosmetic) {
                throw new IllegalStateException("Main Group may not be cosmetic!");
            }
        }

        public static Layout grid(int rows, int columns, int width, int height) {
            return new Layout(Group.grid(rows, columns, 0, 0, width, height, false, true));
        }

        public static Layout fromJson(JsonObject jsonObject) throws JsonParseException {
            return new Layout(Group.fromJson(jsonObject.getAsJsonObject("main")), jsonObject.has("locked") ? Group.fromJson(jsonObject.getAsJsonObject("locked")) : null, jsonObject.has("more") ? Group.fromJson(jsonObject.getAsJsonArray("more")) : new Group[0]);
        }

        public static class Group {
            private final Pos[] positions;
            private final boolean cosmetic;
            private final boolean instance_background;

            public Group(Pos[] positions, boolean cosmetic, boolean instance_background) {
                this.positions = positions;
                this.cosmetic = cosmetic;
                this.instance_background = instance_background;
            }

            public Pos getPos(int index) {
                if (index < 0 || index >= this.positions.length) {
                    return null;
                }
                return this.positions[index];
            }

            public int size() {
                return this.positions.length;
            }

            public static Group grid(int rows, int columns, int x, int y, int width, int height, boolean cosmetic, boolean instance_background) {
                Pos[] positions = new Pos[rows * columns];
                for (int row = 0; row < rows; row++) {
                    for (int column = 0; column < columns; column++) {
                        positions[row * columns + column] = new Pos(x + column * width / columns, y + row * height / rows, width / columns, height / rows);
                    }
                }
                return new Group(positions, cosmetic, instance_background);
            }

            public static Group[] fromJson(JsonArray jsonArray) {
                Group[] groups = new Group[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    groups[i] = Group.fromJson(jsonArray.get(i).getAsJsonObject());
                }
                return groups;
            }

            public static Group fromJson(JsonObject jsonObject) throws JsonParseException {
                boolean cosmetic = jsonObject.has("cosmetic") && jsonObject.get("cosmetic").getAsBoolean();
                boolean instance_background = !jsonObject.has("instance_background") || jsonObject.get("instance_background").getAsBoolean();
                if (jsonObject.has("positions")) {
                    JsonArray positionsArray = jsonObject.get("positions").getAsJsonArray();
                    Pos[] positions = new Pos[positionsArray.size()];
                    for (int i = 0; i < positionsArray.size(); i++) {
                        positions[i] = (Pos.fromJson(positionsArray.get(i).getAsJsonObject()));
                    }
                    return new Group(positions, cosmetic, instance_background);
                }
                return Group.grid(jsonObject.get("rows").getAsInt(), jsonObject.get("columns").getAsInt(), jsonObject.get("x").getAsInt(), jsonObject.get("y").getAsInt(), jsonObject.get("width").getAsInt(), jsonObject.get("height").getAsInt(), cosmetic, instance_background);
            }
        }

        public static class Pos {
            public final int x;
            public final int y;
            public final int width;
            public final int height;

            public Pos(int x, int y, int width, int height) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }

            private static Pos fromJson(JsonObject jsonObject) throws JsonParseException {
                return new Pos(jsonObject.get("x").getAsInt(), jsonObject.get("y").getAsInt(), jsonObject.get("width").getAsInt(), jsonObject.get("height").getAsInt());
            }
        }
    }
}
