package me.contaria.seedqueue.gui.wall;

import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.SeedQueueProfiler;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.SeedQueueSettingsCache;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.customization.Layout;
import me.contaria.seedqueue.customization.LockTexture;
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
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class SeedQueueWallScreen extends Screen {
    private static final Set<WorldRenderer> WORLD_RENDERERS = new HashSet<>();

    public static final Identifier CUSTOM_LAYOUT = new Identifier("seedqueue", "wall/custom_layout.json");
    private static final Identifier WALL_BACKGROUND = new Identifier("seedqueue", "textures/gui/wall/background.png");
    private static final Identifier WALL_OVERLAY = new Identifier("seedqueue", "textures/gui/wall/overlay.png");
    private static final Identifier INSTANCE_BACKGROUND = new Identifier("seedqueue", "textures/gui/wall/instance_background.png");

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

    private int ticks;

    protected int frame;
    private int nextSoundTick;

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
        this.layout = Layout.createLayout();
        this.mainPreviews = new SeedQueuePreview[this.layout.main.size()];
        this.lockedPreviews = this.layout.locked != null ? new ArrayList<>() : null;
        this.preparingPreviews = new ArrayList<>();
        this.lockTextures = LockTexture.createLockTextures();
    }

    protected LockTexture getLockTexture() {
        return this.lockTextures.get(new Random().nextInt(this.lockTextures.size()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        assert this.client != null;
        this.frame++;

        SeedQueueProfiler.swap("wall");

        SeedQueueProfiler.push("update_previews");
        this.updatePreviews();

        SeedQueueProfiler.swap("background");
        if (!this.drawTextureIfPresent(WALL_BACKGROUND, matrices, this.width, this.height)) {
            this.renderBackground(matrices);
        }

        SeedQueueProfiler.swap("render_main");
        for (int i = 0; i < this.layout.main.size(); i++) {
            this.renderInstance(this.mainPreviews[i], this.layout.main, this.layout.main.getPos(i), matrices, delta);
        }
        if (this.layout.locked != null && this.lockedPreviews != null) {
            SeedQueueProfiler.swap("render_locked");
            for (int i = 0; i < this.layout.locked.size(); i++) {
                this.renderInstance(i < this.lockedPreviews.size() ? this.lockedPreviews.get(i) : null, this.layout.locked, this.layout.locked.getPos(i), matrices, delta);
            }
        }
        int i = 0;
        SeedQueueProfiler.swap("render_preparing");
        for (Layout.Group group : this.layout.preparing) {
            int offset = i;
            for (; i < group.size(); i++) {
                this.renderInstance(i < this.preparingPreviews.size() ? this.preparingPreviews.get(i) : null, group, group.getPos(i - offset), matrices, delta);
            }
        }

        SeedQueueProfiler.swap("build_preparing");
        for (; i < this.preparingPreviews.size(); i++) {
            SeedQueuePreview preparingInstance = this.preparingPreviews.get(i);
            SeedQueueProfiler.push("load_settings");
            this.loadPreviewSettings(preparingInstance);
            SeedQueueProfiler.swap("build");
            preparingInstance.build();
            SeedQueueProfiler.pop();
        }

        SeedQueueProfiler.swap("overlay");
        this.drawTextureIfPresent(WALL_OVERLAY, matrices, this.width, this.height);

        SeedQueueProfiler.swap("reset");
        this.resetViewport();
        this.loadPreviewSettings(this.settingsCache, 0);

        if (this.debugHud != null) {
            SeedQueueProfiler.swap("fps_graph");
            ((DebugHudAccessor) this.debugHud).seedQueue$drawMetricsData(matrices, this.client.getMetricsData(), 0, this.width / 2, true);
        }
        SeedQueueProfiler.pop();
    }

    private void renderInstance(SeedQueuePreview instance, Layout.Group group, Layout.Pos pos, MatrixStack matrices, float delta) {
        assert this.client != null;
        if (pos == null) {
            return;
        }
        try {
            SeedQueueProfiler.push("set_viewport");
            this.setViewport(pos);
            if (instance == null || (SeedQueue.config.waitForPreviewSetup && !instance.isPreviewReady())) {
                SeedQueueProfiler.swap("instance_background");
                if (!SeedQueue.config.waitForPreviewSetup && this.layout.main == group) {
                    this.renderBackground(matrices);
                } else if (group.instance_background) {
                    this.drawTextureIfPresent(INSTANCE_BACKGROUND, matrices, this.width, this.height);
                }
                if (instance != null) {
                    SeedQueueProfiler.swap("build_chunks");
                    instance.build();
                }
                SeedQueueProfiler.pop();
                return;
            }
            SeedQueueProfiler.swap("load_settings");
            this.loadPreviewSettings(instance);
            SeedQueueProfiler.swap("render_preview");
            instance.render(matrices, 0, 0, delta);
        } finally {
            SeedQueueProfiler.swap("reset_viewport");
            this.resetViewport();
        }
        if (instance.getSeedQueueEntry().isLocked()) {
            SeedQueueProfiler.swap("lock");
            this.drawLock(matrices, pos, instance.getLockTexture());
        }
        SeedQueueProfiler.pop();
    }

    private void drawLock(MatrixStack matrices, Layout.Pos pos, LockTexture lock) {
        assert this.client != null;
        this.setOrtho(this.client.getWindow().getWidth(), this.client.getWindow().getHeight());
        this.client.getTextureManager().bindTexture(lock.getId());
        DrawableHelper.drawTexture(
                matrices,
                pos.x,
                pos.y,
                0.0f,
                lock.getFrameIndex(this.ticks) * pos.height,
                (int) Math.min(pos.width, pos.height * lock.getAspectRatio()),
                pos.height,
                (int) (pos.height * lock.getAspectRatio()),
                pos.height * lock.getIndividualFrameCount()
        );
        this.resetOrtho();
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
        this.preparingPreviews.sort(Comparator.comparing(SeedQueuePreview::isPreviewReady, Comparator.reverseOrder()));
        for (int i = 0; i < this.mainPreviews.length && !this.preparingPreviews.isEmpty(); i++) {
            if (SeedQueue.config.waitForPreviewSetup && !this.preparingPreviews.get(0).isPreviewReady()) {
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
        List<SeedQueueEntry> entries = SeedQueue.getEntries();
        for (SeedQueuePreview instance: this.getInstances()) {
            entries.remove(instance.getSeedQueueEntry());
        }
        entries.removeIf(entry -> entry.getWorldGenerationProgressTracker() == null);
        if (SeedQueue.config.waitForPreviewSetup) {
            entries.removeIf(entry -> !entry.hasWorldPreview());
        }
        return entries;
    }

    private void loadPreviewSettings(SeedQueuePreview instance) {
        SeedQueueEntry entry = instance.getSeedQueueEntry();
        if (entry.getSettingsCache() != null) {
            this.loadPreviewSettings(entry.getSettingsCache(), entry.getPerspective());
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
        if (instance.hasPreviewRendered() && this.playInstance(entry)) {
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
        if (instance.hasPreviewRendered() && instance.getSeedQueueEntry().lock()) {
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
        if (instance == null || !instance.canReset(ignoreLock, ignoreResetCooldown)) {
            return false;
        }

        SeedQueueProfiler.push("reset_instance");
        SeedQueueProfiler.push("discard_entry");
        SeedQueue.discard(instance.getSeedQueueEntry());

        SeedQueueProfiler.swap("remove_preview");
        this.removePreview(instance);

        if (playSound) {
            SeedQueueProfiler.swap("play_sound");
            this.playSound(SeedQueueSounds.RESET_INSTANCE);
        }
        SeedQueueProfiler.pop();
        SeedQueueProfiler.pop();
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

    private boolean drawTextureIfPresent(Identifier texture, MatrixStack matrices, int width, int height) {
        return this.drawTextureIfPresent(texture, matrices, 0, 0, 0.0f, 0.0f, width, height, width, height);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean drawTextureIfPresent(Identifier texture, MatrixStack matrices, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        assert this.client != null;
        if (this.client.getResourceManager().containsResource(texture)) {
            this.client.getTextureManager().bindTexture(texture);
            RenderSystem.enableBlend();
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
        if (this.nextSoundTick < this.ticks) {
            this.client.getSoundManager().play(soundInstance);
            this.nextSoundTick = this.ticks;
        } else {
            this.client.getSoundManager().play(soundInstance, ++this.nextSoundTick - this.ticks);
        }
        return true;
    }

    @Override
    public void tick() {
        this.ticks++;
    }

    public void populateResetCooldowns() {
        long cooldownStart = System.currentTimeMillis();
        for (SeedQueuePreview instance : this.getInstances()) {
            instance.populateCooldownStart(cooldownStart);
        }
    }

    private void startBenchmark() {
        assert this.client != null;
        for (SeedQueueEntry entry : SeedQueue.getEntries()) {
            SeedQueue.discard(entry);
        }
        for (SeedQueuePreview instance : this.getInstances()) {
            this.removePreview(instance);
        }
        this.benchmarkGoal = SeedQueue.config.benchmarkResets;
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
            SeedQueueProfiler.push("world_renderer_clear");
            worldRenderer.setWorld(null);
            SeedQueueProfiler.pop();
        }
    }

    private static ClientWorld getWorld(WorldRenderer worldRenderer) {
        return ((WorldRendererAccessor) worldRenderer).seedQueue$getWorld();
    }

}
