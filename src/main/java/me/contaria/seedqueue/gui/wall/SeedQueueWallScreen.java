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
import me.contaria.seedqueue.mixin.accessor.DebugHudAccessor;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.contaria.seedqueue.sounds.SeedQueueSounds;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
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
import net.minecraft.util.profiler.Profiler;
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

    private final DebugHud debugHud;

    protected final SeedQueueSettingsCache settingsCache;
    private SeedQueueSettingsCache lastSettingsCache;

    private Layout layout;
    private SeedQueuePreview[] mainPreviews;
    @Nullable
    private List<SeedQueuePreview> lockedPreviews;
    private List<SeedQueuePreview> preparingPreviews;

    private final Set<Integer> blockedMainPositions = new HashSet<>();

    private List<LockTexture> lockTextures;

    protected int frame;
    private int nextSoundFrame;

    @Nullable
    private Layout.Pos currentPos;

    private final long benchmarkStart = System.currentTimeMillis();
    private int benchmarkedSeeds;

    public SeedQueueWallScreen(Screen createWorldScreen) {
        super(LiteralText.EMPTY);
        this.createWorldScreen = createWorldScreen;
        this.debugHud = new DebugHud(MinecraftClient.getInstance());
        this.preparingPreviews = new ArrayList<>(SeedQueue.config.backgroundPreviews);
        this.lastSettingsCache = this.settingsCache = SeedQueueSettingsCache.create();
    }

    @Override
    protected void init() {
        assert this.client != null;
        this.layout = this.createLayout();
        this.mainPreviews = new SeedQueuePreview[this.layout.main.size()];
        this.lockedPreviews = this.layout.locked != null ? new ArrayList<>() : null;
        this.preparingPreviews = new ArrayList<>();
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

        Profiler profiler = this.client.getProfiler();
        profiler.swap("wall");

        profiler.push("update_previews");
        this.updatePreviews();

        profiler.swap("background");
        if (this.client.getResourceManager().containsResource(WALL_BACKGROUND)) {
            this.client.getTextureManager().bindTexture(WALL_BACKGROUND);
            DrawableHelper.drawTexture(matrices, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        } else {
            this.renderBackground(matrices);
        }

        profiler.swap("render_main");
        for (int i = 0; i < this.layout.main.size(); i++) {
            this.renderInstance(this.mainPreviews[i], this.layout.main, this.layout.main.getPos(i), matrices, delta);
        }
        if (this.layout.locked != null && this.lockedPreviews != null) {
            profiler.swap("render_locked");
            for (int i = 0; i < this.layout.locked.size(); i++) {
                this.renderInstance(i < this.lockedPreviews.size() ? this.lockedPreviews.get(i) : null, this.layout.locked, this.layout.locked.getPos(i), matrices, delta);
            }
        }
        int i = 0;
        profiler.swap("render_more");
        for (Layout.Group group : this.layout.more) {
            int offset = i;
            for (; i < group.size(); i++) {
                this.renderInstance(i < this.preparingPreviews.size() ? this.preparingPreviews.get(i) : null, group, group.getPos(i - offset), matrices, delta);
            }
        }

        profiler.swap("build_more");
        for (; i < this.preparingPreviews.size(); i++) {
            SeedQueuePreview preparingInstance = this.preparingPreviews.get(i);
            profiler.push("load_settings");
            this.loadPreviewSettings(preparingInstance);
            profiler.swap("build");
            preparingInstance.buildChunks();
            profiler.pop();
        }

        profiler.swap("overlay");
        if (this.client.getResourceManager().containsResource(WALL_OVERLAY)) {
            this.client.getTextureManager().bindTexture(WALL_OVERLAY);
            DrawableHelper.drawTexture(matrices, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        }

        profiler.swap("reset");
        this.resetViewport();
        this.loadPreviewSettings(this.settingsCache, 0);

        if (SeedQueue.config.showDebugMenu) {
            profiler.swap("fps_graph");
            ((DebugHudAccessor) this.debugHud).seedQueue$drawMetricsData(matrices, this.client.getMetricsData(), 0, this.width / 2, true);
        }
        profiler.pop();
    }

    private void renderInstance(SeedQueuePreview instance, Layout.Group group, Layout.Pos pos, MatrixStack matrices, float delta) {
        assert this.client != null;
        if (pos == null) {
            return;
        }
        Profiler profiler = this.client.getProfiler();
        try {
            profiler.push("set_viewport");
            this.setViewport(pos);
            if (instance == null || !instance.shouldRender()) {
                if (group.instance_background && this.client.getResourceManager().containsResource(INSTANCE_BACKGROUND)) {
                    profiler.swap("instance_background");
                    this.client.getTextureManager().bindTexture(INSTANCE_BACKGROUND);
                    DrawableHelper.drawTexture(matrices, 0, 0, 0.0f, 0.0f, this.width, this.height, this.width, this.height);
                }
                profiler.pop();
                return;
            }
            profiler.swap("load_settings");
            this.loadPreviewSettings(instance);
            profiler.swap("render_preview");
            instance.render(matrices, 0, 0, delta);
        } finally {
            profiler.swap("reset_viewport");
            this.resetViewport();
        }
        if (instance.getSeedQueueEntry().isLocked()) {
            profiler.swap("lock");
            this.renderLock(instance, pos, matrices);
        }
        profiler.pop();
    }

    private void renderLock(SeedQueuePreview instance, Layout.Pos pos, MatrixStack matrices) {
        assert this.client != null;
        if (!this.lockTextures.isEmpty()) {
            if (instance.lock == null) {
                instance.lock = this.lockTextures.get(new Random().nextInt(this.lockTextures.size()));
            }
            this.client.getTextureManager().bindTexture(instance.lock.id);
            double scale = this.client.getWindow().getScaleFactor();
            DrawableHelper.drawTexture(matrices, (int) (pos.x / scale), (int) (pos.y / scale), 0.0f, 0.0f, (int) (pos.height * instance.lock.aspectRatio / scale), (int) (pos.height / scale), (int) (pos.height * instance.lock.aspectRatio / scale), (int) (pos.height / scale));
        }
    }

    private void setViewport(Layout.Pos pos) {
        assert this.client != null;
        RenderSystem.viewport(pos.x, this.client.getWindow().getFramebufferHeight() - pos.height - pos.y, pos.width, pos.height);

        this.currentPos = pos;
    }

    public void refreshViewport() {
        if (this.currentPos != null) {
            this.setViewport(this.currentPos);
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

        this.currentPos = null;
    }

    private void updatePreviews() {
        this.updateLockedPreviews();
        this.updateMainPreviews();
        this.updatePreparingPreviews();
    }

    private void updateLockedPreviews() {
        if (this.lockedPreviews != null) {
            for (SeedQueueEntry entry : this.getAvailableSeedQueueEntries()) {
                if (entry.isLocked()) {
                    this.lockedPreviews.add(new SeedQueuePreview(this, entry));
                }
            }
            for (int i = 0; i < this.mainPreviews.length; i++) {
                SeedQueuePreview instance = this.mainPreviews[i];
                if (instance != null && instance.getSeedQueueEntry().isLocked()) {
                    this.lockedPreviews.add(instance);
                    this.mainPreviews[i] = null;
                    if (!SeedQueue.config.replaceLockedPreviews) {
                        this.blockedMainPositions.add(i);
                    }
                }
            }
            for (SeedQueuePreview instance : this.preparingPreviews) {
                if (instance.getSeedQueueEntry().isLocked()) {
                    this.lockedPreviews.add(instance);
                }
            }
            this.preparingPreviews.removeAll(this.lockedPreviews);
        }
    }

    private void updateMainPreviews() {
        this.preparingPreviews.sort(Comparator.comparing(SeedQueuePreview::shouldRender, Comparator.reverseOrder()));
        for (int i = 0; i < this.mainPreviews.length && !this.preparingPreviews.isEmpty() && this.preparingPreviews.get(0).shouldRender(); i++) {
            if (this.mainPreviews[i] == null && !this.blockedMainPositions.contains(i)) {
                this.preparingPreviews.remove(this.mainPreviews[i] = this.preparingPreviews.remove(0));
            }
        }
    }

    private void updatePreparingPreviews() {
        int urgent = (int) Arrays.stream(this.mainPreviews).filter(Objects::isNull).count() - Math.min(this.blockedMainPositions.size(), this.preparingPreviews.size());
        int capacity = SeedQueue.config.backgroundPreviews + urgent;
        if (this.preparingPreviews.size() < capacity) {
            int budget = Math.max(1, urgent);
            for (SeedQueueEntry entry : this.getAvailableSeedQueueEntries()) {
                this.preparingPreviews.add(new SeedQueuePreview(this, entry));
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
        entries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldGenerationProgressTracker() == null || seedQueueEntry.getWorldPreviewProperties() == null);
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

        // see Keyboard#onKey
        if (SeedQueue.config.showDebugMenu && keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            ((MinecraftClientAccessor) this.client).seedQueue$handleProfilerKeyPress(keyCode - GLFW.GLFW_KEY_0);
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
            Optional<SeedQueuePreview> instance = this.getInstance(this.layout.more[i], x, y).filter(index -> index < this.preparingPreviews.size()).map(this.preparingPreviews::get);
            if (instance.isPresent()) {
                return instance.get();
            }
        }
        if (this.layout.locked != null && this.lockedPreviews != null) {
            Optional<SeedQueuePreview> instance = this.getInstance(this.layout.locked, x, y).filter(index -> index < this.lockedPreviews.size()).map(this.lockedPreviews::get);
            if (instance.isPresent()) {
                return instance.get();
            }
        }
        return this.getInstance(this.layout.main, x, y).map(index -> this.mainPreviews[index]).orElse(null);
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
        for (SeedQueuePreview instance : this.mainPreviews) {
            if (instance != null) {
                instances.add(instance);
            }
        }
        instances.addAll(this.preparingPreviews);
        if (this.lockedPreviews != null) {
            instances.addAll(this.lockedPreviews);
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
        if (MinecraftClient.getInstance().getServer() != null || !entry.isReady() || SeedQueue.selectedEntry != null) {
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
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        if (instance == null) {
            return false;
        }
        profiler.push("reset_instance");
        SeedQueueEntry seedQueueEntry = instance.getSeedQueueEntry();
        if (!instance.hasBeenRendered() || (seedQueueEntry.isLocked() && !ignoreLock) || System.currentTimeMillis() - instance.firstRenderTime < SeedQueue.config.resetCooldown || SeedQueue.selectedEntry == seedQueueEntry) {
            profiler.pop();
            return false;
        }

        profiler.push("discard_entry");
        SeedQueue.discard(seedQueueEntry);

        profiler.swap("remove_preview");
        for (int i = 0; i < this.mainPreviews.length; i++) {
            if (this.mainPreviews[i] == instance) {
                this.mainPreviews[i] = null;
            }
        }
        this.preparingPreviews.remove(instance);
        if (this.lockedPreviews != null) {
            this.lockedPreviews.remove(instance);
        }

        profiler.swap("play_sound");
        this.playSound(SeedQueueSounds.RESET_INSTANCE);
        profiler.pop();
        profiler.pop();
        return true;
    }

    private void resetAllInstances() {
        for (SeedQueuePreview instance : this.mainPreviews) {
            this.resetInstance(instance);
        }
        this.blockedMainPositions.clear();
    }

    private void resetColumn(double mouseX) {
        assert this.client != null;
        double x = mouseX * this.client.getWindow().getScaleFactor();
        for (int i = 0; i < this.mainPreviews.length; i++) {
            Layout.Pos pos = this.layout.main.getPos(i);
            if (x >= pos.x && x <= pos.x + pos.width) {
                this.resetInstance(this.mainPreviews[i]);
            }
        }
    }

    private void resetRow(double mouseY) {
        assert this.client != null;
        double y = mouseY * this.client.getWindow().getScaleFactor();
        for (int i = 0; i < this.mainPreviews.length; i++) {
            Layout.Pos pos = this.layout.main.getPos(i);
            if (y >= pos.y && y <= pos.y + pos.height) {
                this.resetInstance(this.mainPreviews[i]);
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

    public void populateResetCooldowns() {
        long renderTime = System.currentTimeMillis();
        for (SeedQueuePreview instance : this.getInstances()) {
            if (instance.firstRenderFrame == this.frame) {
                instance.firstRenderTime = renderTime;
            }
        }
    }

    public void tickBenchmark() {
        assert this.client != null;
        if (!this.isBenchmarking()) {
            return;
        }
        for (SeedQueuePreview instance : this.mainPreviews) {
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
            Profiler profiler = MinecraftClient.getInstance().getProfiler();
            profiler.push("world_renderer_clear");
            worldRenderer.setWorld(null);
            profiler.pop();
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
