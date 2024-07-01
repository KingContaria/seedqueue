package me.contaria.seedqueue.gui.wall;

import com.google.gson.*;
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
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class SeedQueueWallScreen extends Screen {
    private static final Set<WorldRenderer> WORLD_RENDERERS = new HashSet<>();

    private static final Identifier WALL_BACKGROUND = new Identifier("seedqueue", "textures/gui/wall/background.png");
    private static final Identifier WALL_OVERLAY = new Identifier("seedqueue", "textures/gui/wall/overlay.png");
    private static final Identifier INSTANCE_BACKGROUND = new Identifier("seedqueue", "textures/gui/wall/instance_background.png");
    private static final Identifier CUSTOM_LAYOUT = new Identifier("seedqueue", "wall/custom_layout.json");

    private final Screen createWorldScreen;

    @Nullable
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

    protected long benchmarkStart;
    protected int benchmarkedSeeds;
    protected int benchmarkGoal;
    protected long benchmarkFinish;

    public SeedQueueWallScreen(Screen createWorldScreen) {
        super(LiteralText.EMPTY);
        this.createWorldScreen = createWorldScreen;
        this.debugHud = SeedQueue.config.showDebugMenu ? new DebugHud(MinecraftClient.getInstance()) : null;
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
        if (this.client.getResourceManager().containsResource(CUSTOM_LAYOUT)) {
            try (Reader reader = new InputStreamReader(this.client.getResourceManager().getResource(CUSTOM_LAYOUT).getInputStream(), StandardCharsets.UTF_8)) {
                return Layout.fromJson(new JsonParser().parse(reader).getAsJsonObject());
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
        if (!this.drawTexture(WALL_BACKGROUND, matrices, this.width, this.height)) {
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
        profiler.swap("render_preparing");
        for (Layout.Group group : this.layout.preparing) {
            int offset = i;
            for (; i < group.size(); i++) {
                this.renderInstance(i < this.preparingPreviews.size() ? this.preparingPreviews.get(i) : null, group, group.getPos(i - offset), matrices, delta);
            }
        }

        profiler.swap("build_preparing");
        for (; i < this.preparingPreviews.size(); i++) {
            SeedQueuePreview preparingInstance = this.preparingPreviews.get(i);
            profiler.push("load_settings");
            this.loadPreviewSettings(preparingInstance);
            profiler.swap("build");
            preparingInstance.build();
            profiler.pop();
        }

        profiler.swap("overlay");
        this.drawTexture(WALL_OVERLAY, matrices, this.width, this.height);

        profiler.swap("reset");
        this.resetViewport();
        this.loadPreviewSettings(this.settingsCache, 0);

        if (this.debugHud != null) {
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
            if (instance == null || (SeedQueue.config.waitForPreviewSetup && !instance.shouldRender())) {
                if (!SeedQueue.config.waitForPreviewSetup && this.layout.main == group) {
                    this.renderBackground(matrices);
                } else if (group.instance_background) {
                    profiler.swap("instance_background");
                    this.drawTexture(INSTANCE_BACKGROUND, matrices, this.width, this.height);
                }
                if (instance != null) {
                    profiler.swap("build_chunks");
                    instance.build();
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
            this.setOrtho(this.client.getWindow().getWidth(), this.client.getWindow().getHeight());
            this.drawTexture(
                    instance.lock.id,
                    matrices,
                    pos.x,
                    pos.y,
                    0.0f,
                    0.0f,
                    (int) Math.min(pos.width, pos.height * instance.lock.aspectRatio),
                    pos.height,
                    (int) (pos.height * instance.lock.aspectRatio),
                    pos.height
            );
            this.resetOrtho();
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

    private void resetViewport() {
        assert this.client != null;
        Window window = this.client.getWindow();
        RenderSystem.viewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
        this.setOrtho((double) window.getFramebufferWidth() / window.getScaleFactor(), (double) window.getFramebufferHeight() / window.getScaleFactor());
        this.currentPos = null;
    }

    @SuppressWarnings("deprecation")
    protected void setOrtho(double width, double height) {
        // see GameRenderer#render or WorldPreview#render
        // we need this to reset RenderSystem.ortho after simulating a different window size
        RenderSystem.clear(256, MinecraftClient.IS_SYSTEM_MAC);
        RenderSystem.matrixMode(5889);
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(5888);
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
    }

    private void resetOrtho() {
        this.setOrtho(this.width, this.height);
    }

    private void updatePreviews() {
        this.updateLockedPreviews();
        this.updateMainPreviews();
        this.updatePreparingPreviews();
    }

    private void updateLockedPreviews() {
        if (this.lockedPreviews == null) {
            return;
        }
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
                if (!this.layout.replaceLockedInstances) {
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

    private void updateMainPreviews() {
        this.preparingPreviews.sort(Comparator.comparing(SeedQueuePreview::shouldRender, Comparator.reverseOrder()));
        for (int i = 0; i < this.mainPreviews.length && !this.preparingPreviews.isEmpty(); i++) {
            if (SeedQueue.config.waitForPreviewSetup && !this.preparingPreviews.get(0).shouldRender()) {
                break;
            }
            if (this.mainPreviews[i] == null && !this.blockedMainPositions.contains(i)) {
                this.mainPreviews[i] = this.preparingPreviews.remove(0);
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
        entries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldGenerationProgressTracker() == null);
        if (SeedQueue.config.waitForPreviewSetup) {
            entries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldPreviewProperties() == null);
        }
        return entries;
    }

    private void loadPreviewSettings(SeedQueuePreview instance) {
        WorldPreviewProperties wpProperties = instance.getWorldPreviewProperties();
        if (wpProperties != null) {
            this.loadPreviewSettings(wpProperties.getSettingsCache(), wpProperties.getPerspective());
        } else {
            this.loadPreviewSettings(this.settingsCache, 0);
        }
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
            if (SeedQueueKeyBindings.cancelBenchmark.matchesMouse(button)) {
                this.stopBenchmark();
            }
            return true;
        }

        if (SeedQueueKeyBindings.startBenchmark.matchesMouse(button)) {
            this.startBenchmark();
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
            this.playNextLock();
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
            this.resetInstance(instance, true, false, true);
        }
        if (SeedQueueKeyBindings.focusReset.matchesMouse(button)) {
            this.focusReset(instance);
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
            if (SeedQueueKeyBindings.cancelBenchmark.matchesKey(keyCode, scanCode)) {
                this.stopBenchmark();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && Screen.hasShiftDown()) {
            ModCompat.standardsettings$onWorldJoin();
            Atum.stopRunning();
            this.client.openScreen(new TitleScreen());
            return true;
        }

        if (SeedQueueKeyBindings.startBenchmark.matchesKey(keyCode, scanCode)) {
            this.startBenchmark();
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
            this.playNextLock();
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

        if (SeedQueue.config.showDebugMenu && keyCode == GLFW.GLFW_KEY_F3) {
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
            this.resetInstance(instance, true, false, true);
        }
        if (SeedQueueKeyBindings.focusReset.matchesKey(keyCode, scanCode)) {
            this.focusReset(instance);
        }
        return true;
    }

    private SeedQueuePreview getInstance(double mouseX, double mouseY) {
        assert this.client != null;
        double scale = this.client.getWindow().getScaleFactor();
        double x = mouseX * scale;
        double y = mouseY * scale;

        // we traverse the layout in reverse to catch the top rendered instance
        for (int i = this.layout.preparing.length - 1; i >= 0; i--) {
            Optional<SeedQueuePreview> instance = this.getInstance(this.layout.preparing[i], x, y).filter(index -> index < this.preparingPreviews.size()).map(this.preparingPreviews::get);
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
        assert this.client != null;
        SeedQueueEntry entry = instance.getSeedQueueEntry();
        if (instance.hasBeenRendered() && this.playInstance(entry)) {
            this.removePreview(instance);
        }
    }

    private boolean playInstance(SeedQueueEntry entry) {
        assert this.client != null;
        if (this.client.getServer() != null || SeedQueue.selectedEntry != null || !entry.isReady()) {
            return false;
        }
        this.playSound(SeedQueueSounds.PLAY_INSTANCE);
        SeedQueue.comingFromWall = true;
        SeedQueue.selectedEntry = entry;
        this.client.openScreen(this.createWorldScreen);
        return true;
    }

    private void lockInstance(SeedQueuePreview instance) {
        if (instance.hasBeenRendered() && instance.getSeedQueueEntry().lock()) {
            if (SeedQueue.config.freezeLockedPreviews) {
                // clearing WorldPreviewProperties frees the previews WorldRenderer, allowing resources to be cleared
                // it also means the amount of WorldRenderers does not exceed Rows * Columns + Background Previews
                // when a custom layout with a locked group is used
                instance.getSeedQueueEntry().setWorldPreviewProperties(null);
            }
            this.playSound(SeedQueueSounds.LOCK_INSTANCE);
        }
    }

    private boolean resetInstance(SeedQueuePreview instance, boolean ignoreLock, boolean ignoreResetCooldown, boolean playSound) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();

        if (instance == null) {
            return false;
        }
        profiler.push("reset_instance");
        SeedQueueEntry seedQueueEntry = instance.getSeedQueueEntry();
        if (!instance.hasBeenRendered() || (seedQueueEntry.isLocked() && !ignoreLock) || (System.currentTimeMillis() - instance.firstRenderTime < SeedQueue.config.resetCooldown && !ignoreResetCooldown) || SeedQueue.selectedEntry == seedQueueEntry) {
            profiler.pop();
            return false;
        }

        profiler.push("discard_entry");
        SeedQueue.discard(seedQueueEntry);

        profiler.swap("remove_preview");
        this.removePreview(instance);

        if (playSound) {
            profiler.swap("play_sound");
            this.playSound(SeedQueueSounds.RESET_INSTANCE);
        }
        profiler.pop();
        profiler.pop();
        return true;
    }

    private SeedQueuePreview getPreview(SeedQueueEntry entry) {
        for (SeedQueuePreview preview : this.getInstances()) {
            if (entry == preview.getSeedQueueEntry()) {
                return preview;
            }
        }
        return null;
    }

    private void removePreview(SeedQueuePreview preview) {
        if (preview == null) {
            return;
        }
        for (int i = 0; i < this.mainPreviews.length; i++) {
            if (this.mainPreviews[i] == preview) {
                this.mainPreviews[i] = null;
            }
        }
        this.preparingPreviews.remove(preview);
        if (this.lockedPreviews != null) {
            this.lockedPreviews.remove(preview);
        }
    }

    private void resetAllInstances() {
        boolean playSound = !this.playSound(SeedQueueSounds.RESET_ALL);
        for (SeedQueuePreview instance : this.mainPreviews) {
            this.resetInstance(instance, false, false, playSound);
        }
        this.blockedMainPositions.clear();
    }

    private void focusReset(SeedQueuePreview instance) {
        if (instance.getSeedQueueEntry().isReady()) {
            this.playInstance(instance);
        } else {
            this.lockInstance(instance);
        }
        this.resetAllInstances();
    }

    private void resetColumn(double mouseX) {
        assert this.client != null;
        double x = mouseX * this.client.getWindow().getScaleFactor();
        boolean playSound = !this.playSound(SeedQueueSounds.RESET_COLUMN);
        for (int i = 0; i < this.mainPreviews.length; i++) {
            Layout.Pos pos = this.layout.main.getPos(i);
            if (x >= pos.x && x <= pos.x + pos.width) {
                this.resetInstance(this.mainPreviews[i], false, false, playSound);
            }
        }
    }

    private void resetRow(double mouseY) {
        assert this.client != null;
        double y = mouseY * this.client.getWindow().getScaleFactor();
        boolean playSound = !this.playSound(SeedQueueSounds.RESET_ROW);
        for (int i = 0; i < this.mainPreviews.length; i++) {
            Layout.Pos pos = this.layout.main.getPos(i);
            if (y >= pos.y && y <= pos.y + pos.height) {
                this.resetInstance(this.mainPreviews[i], false, false, playSound);
            }
        }
    }

    private void playNextLock() {
        SeedQueue.getEntryMatching(entry -> entry.isLocked() && entry.isReady()).ifPresent(entry -> {
            this.playInstance(entry);
            this.removePreview(this.getPreview(entry));
        });
    }

    private boolean drawTexture(Identifier texture, MatrixStack matrices, int width, int height) {
        return this.drawTexture(texture, matrices, 0, 0, 0.0f, 0.0f, width, height, width, height);
    }

    private boolean drawTexture(Identifier texture, MatrixStack matrices, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        assert this.client != null;
        if (this.client.getResourceManager().containsResource(texture)) {
            RenderSystem.enableBlend();
            this.client.getTextureManager().bindTexture(texture);
            DrawableHelper.drawTexture(matrices, x, y, u, v, width, height, textureWidth, textureHeight);
            RenderSystem.disableBlend();
            return true;
        }
        return false;
    }

    private boolean playSound(SoundEvent sound) {
        assert this.client != null;
        SoundInstance soundInstance = PositionedSoundInstance.master(sound, 1.0f);
        soundInstance.getSoundSet(this.client.getSoundManager());
        if (soundInstance.getSound().equals(SoundManager.MISSING_SOUND)) {
            return false;
        }
        if (this.nextSoundFrame < this.frame) {
            this.client.getSoundManager().play(soundInstance);
            this.nextSoundFrame = this.frame;
        } else {
            this.client.getSoundManager().play(soundInstance, ++this.nextSoundFrame - this.frame);
        }
        return true;
    }

    public void populateResetCooldowns() {
        long renderTime = System.currentTimeMillis();
        for (SeedQueuePreview instance : this.getInstances()) {
            if (instance.firstRenderFrame == this.frame) {
                instance.firstRenderTime = renderTime;
            }
        }
    }

    private void startBenchmark() {
        assert this.client != null;
        this.benchmarkGoal = SeedQueue.config.benchmarkResets;
        if (this.benchmarkGoal == 0) {
            // show a failure toast here
            SeedQueue.LOGGER.warn("BENCHMARK | Could not start benchmark because Benchmark Resets is set to 0.");
            return;
        }
        for (SeedQueueEntry entry : new HashSet<>(SeedQueue.SEED_QUEUE)) {
            SeedQueue.discard(entry);
        }
        for (SeedQueuePreview instance : this.getInstances()) {
            this.removePreview(instance);
        }
        this.benchmarkStart = System.currentTimeMillis();
        this.benchmarkedSeeds = 0;
        this.client.getToastManager().clear();
        this.client.getToastManager().add(new SeedQueueBenchmarkToast(this));
        this.playSound(SeedQueueSounds.START_BENCHMARK);
    }

    private void stopBenchmark() {
        if (this.isBenchmarking()) {
            this.benchmarkGoal = this.benchmarkedSeeds;
            this.finishBenchmark();
        }
    }

    private void finishBenchmark() {
        this.benchmarkFinish = System.currentTimeMillis();
        SeedQueue.LOGGER.info("BENCHMARK | Reset {} seeds in {} seconds.", this.benchmarkedSeeds, Math.round((this.benchmarkFinish - this.benchmarkStart) / 10.0) / 100.0);
        this.playSound(SeedQueueSounds.FINISH_BENCHMARK);
    }

    public void tickBenchmark() {
        if (!this.isBenchmarking()) {
            return;
        }
        for (SeedQueuePreview instance : this.getInstances()) {
            if (this.resetInstance(instance, true, true, false)) {
                this.benchmarkedSeeds++;
                if (!this.isBenchmarking()) {
                    this.finishBenchmark();
                } else if (this.benchmarkedSeeds % 100 == 0) {
                    SeedQueue.LOGGER.info("BENCHMARK | Reset {} seeds in {} seconds...", this.benchmarkedSeeds, Math.round((System.currentTimeMillis() - this.benchmarkStart) / 10.0) / 100.0);
                }
            }
        }
    }

    protected boolean isBenchmarking() {
        return this.benchmarkedSeeds < this.benchmarkGoal;
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
        private final Group[] preparing;
        private final boolean replaceLockedInstances;

        public Layout(@NotNull Group main) {
            this(main, null, new Group[0], true);
        }

        public Layout(@NotNull Group main, @Nullable Group locked, Group[] preparing, boolean replaceLockedInstances) {
            this.main = main;
            this.locked = locked;
            this.preparing = preparing;
            this.replaceLockedInstances = replaceLockedInstances;

            if (this.main.cosmetic) {
                throw new IllegalStateException("Main Group may not be cosmetic!");
            }
        }

        public static int getX(JsonObject jsonObject) {
            return getAsInt(jsonObject, "x", MinecraftClient.getInstance().getWindow().getWidth());
        }

        public static int getY(JsonObject jsonObject) {
            return getAsInt(jsonObject, "y", MinecraftClient.getInstance().getWindow().getHeight());
        }

        public static int getWidth(JsonObject jsonObject) {
            return getAsInt(jsonObject, "width", MinecraftClient.getInstance().getWindow().getWidth());
        }

        public static int getHeight(JsonObject jsonObject) {
            return getAsInt(jsonObject, "height", MinecraftClient.getInstance().getWindow().getHeight());
        }

        private static int getAsInt(JsonObject jsonObject, String name, int windowSize) {
            JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive(name);
            if (jsonPrimitive.isNumber() && jsonPrimitive.toString().contains(".")) {
                return (int) (windowSize * jsonPrimitive.getAsDouble());
            }
            return jsonPrimitive.getAsInt();
        }

        public static Layout grid(int rows, int columns, int width, int height) {
            return new Layout(Group.grid(rows, columns, 0, 0, width, height, 0, false, true));
        }

        public static Layout fromJson(JsonObject jsonObject) throws JsonParseException {
            return new Layout(
                    Group.fromJson(jsonObject.getAsJsonObject("main"), SeedQueue.config.rows, SeedQueue.config.columns),
                    jsonObject.has("locked") ? Group.fromJson(jsonObject.getAsJsonObject("locked")) : null,
                    jsonObject.has("preparing") ? Group.fromJson(jsonObject.getAsJsonArray("preparing")) : new Group[0],
                    jsonObject.has("replaceLockedInstances") && jsonObject.get("replaceLockedInstances").getAsBoolean()
            );
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

            public static Group grid(int rows, int columns, int x, int y, int width, int height, int padding, boolean cosmetic, boolean instance_background) {
                Pos[] positions = new Pos[rows * columns];
                int columnWidth = (width - padding * (columns - 1)) / columns;
                int rowHeight = (height - padding * (rows - 1)) / rows;
                for (int row = 0; row < rows; row++) {
                    for (int column = 0; column < columns; column++) {
                        positions[row * columns + column] = new Pos(
                                x + column * columnWidth + padding * column,
                                y + row * rowHeight + padding * row,
                                columnWidth,
                                rowHeight
                        );
                    }
                }
                return new Group(positions, cosmetic, instance_background);
            }

            public static Group[] fromJson(JsonArray jsonArray) throws JsonParseException {
                Group[] groups = new Group[jsonArray.size()];
                for (int i = 0; i < jsonArray.size(); i++) {
                    groups[i] = Group.fromJson(jsonArray.get(i).getAsJsonObject());
                }
                return groups;
            }

            public static Group fromJson(JsonObject jsonObject) throws JsonParseException {
                return fromJson(jsonObject, null, null);
            }

            public static Group fromJson(JsonObject jsonObject, Integer defaultRows, Integer defaultColumns) throws JsonParseException {
                boolean cosmetic = jsonObject.has("cosmetic") && jsonObject.get("cosmetic").getAsBoolean();
                boolean instance_background = !jsonObject.has("instance_background") || jsonObject.get("instance_background").getAsBoolean();
                if (jsonObject.has("positions")) {
                    JsonArray positionsArray = jsonObject.get("positions").getAsJsonArray();
                    Pos[] positions = new Pos[positionsArray.size()];
                    for (int i = 0; i < positionsArray.size(); i++) {
                        positions[i] = Pos.fromJson(positionsArray.get(i).getAsJsonObject());
                    }
                    return new Group(positions, cosmetic, instance_background);
                }
                return Group.grid(
                        jsonObject.has("rows") || defaultRows == null ? jsonObject.get("rows").getAsInt() : defaultRows,
                        jsonObject.has("columns") || defaultColumns == null ? jsonObject.get("columns").getAsInt() : defaultColumns,
                        Layout.getX(jsonObject),
                        Layout.getY(jsonObject),
                        Layout.getWidth(jsonObject),
                        Layout.getHeight(jsonObject),
                        jsonObject.has("padding") ? jsonObject.get("padding").getAsInt() : 0, cosmetic, instance_background
                );
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
                return new Pos(
                        Layout.getX(jsonObject),
                        Layout.getY(jsonObject),
                        Layout.getWidth(jsonObject),
                        Layout.getHeight(jsonObject)
                );
            }
        }
    }
}
