
package me.contaria.seedqueue.gui.wall;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.SeedQueueThread;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.compat.SeedQueuePreviewProperties;
import me.contaria.seedqueue.compat.SeedQueueSettingsCache;
import me.contaria.seedqueue.customization.AnimatedTexture;
import me.contaria.seedqueue.customization.Layout;
import me.contaria.seedqueue.customization.LockTexture;
import me.contaria.seedqueue.debug.SeedQueueProfiler;
import me.contaria.seedqueue.keybindings.SeedQueueKeyBindings;
import me.contaria.seedqueue.mixin.accessor.DebugHudAccessor;
import me.contaria.seedqueue.mixin.accessor.MinecraftClientAccessor;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.contaria.seedqueue.sounds.SeedQueueSounds;
import me.contaria.speedrunapi.util.IdentifierUtil;
import me.contaria.speedrunapi.util.TextUtil;
import me.voidxwalker.autoreset.Atum;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.IntStream;

public class SeedQueueWallScreen extends Screen {
    private static final Set<WorldRenderer> WORLD_RENDERERS = new HashSet<>();

    public static final Identifier CUSTOM_LAYOUT = IdentifierUtil.of("seedqueue", "wall/custom_layout.json");
    private static final Identifier WALL_BACKGROUND = IdentifierUtil.of("seedqueue", "textures/gui/wall/background.png");
    private static final Identifier WALL_OVERLAY = IdentifierUtil.of("seedqueue", "textures/gui/wall/overlay.png");
    private static final Identifier INSTANCE_BACKGROUND = IdentifierUtil.of("seedqueue", "textures/gui/wall/instance_background.png");
    private static final Identifier INSTANCE_OVERLAY = IdentifierUtil.of("seedqueue", "textures/gui/wall/instance_overlay.png");

    private static boolean renderingPreview;

    private final MinecraftClient client;
    @Nullable
    private final DebugHud debugHud;
    private final Random random;

    protected final SeedQueueSettingsCache settingsCache;
    private SeedQueueSettingsCache lastSettingsCache;

    protected Layout layout;
    private SeedQueuePreview[] mainPreviews;
    @Nullable
    private List<SeedQueuePreview> lockedPreviews;
    private List<SeedQueuePreview> preparingPreviews;

    private final IntSet blockedMainPositions = new IntOpenHashSet();

    private final Set<SeedQueueEntry> scheduledEntries = new HashSet<>();
    private boolean playedScheduledEnterWarning;

    private List<LockTexture> lockTextures;
    @Nullable
    private AnimatedTexture background;
    @Nullable
    private AnimatedTexture overlay;
    @Nullable
    private AnimatedTexture instanceBackground;
    @Nullable
    private AnimatedTexture instanceOverlay;

    private int ticks;

    protected int frame;
    private int nextResetSoundTick;

    @Nullable
    private Layout.Pos currentPos;

    protected long benchmarkStart;
    protected int benchmarkedSeeds;
    protected int benchmarkGoal;
    protected long benchmarkFinish;
    protected boolean showFinishedBenchmarkResults;

    public SeedQueueWallScreen() {
        super(TextUtil.empty());
        this.client = MinecraftClient.getInstance();
        this.debugHud = SeedQueue.config.showDebugMenu ? new DebugHud(MinecraftClient.getInstance()) : null;
        this.random = new Random();
        this.preparingPreviews = new ArrayList<>();
        this.lastSettingsCache = this.settingsCache = SeedQueueSettingsCache.create();
    }

    @Override
    protected void init() {
        this.layout = Layout.createLayout();
        this.mainPreviews = new SeedQueuePreview[this.layout.main.size()];
        this.lockedPreviews = this.layout.locked != null ? new ArrayList<>() : null;
        this.preparingPreviews = new ArrayList<>();
        this.lockTextures = LockTexture.createLockTextures();
        this.background = AnimatedTexture.of(WALL_BACKGROUND);
        this.overlay = AnimatedTexture.of(WALL_OVERLAY);
        this.instanceBackground = AnimatedTexture.of(INSTANCE_BACKGROUND);
        this.instanceOverlay = AnimatedTexture.of(INSTANCE_OVERLAY);
    }

    protected LockTexture getRandomLockTexture() {
        return this.lockTextures.get(this.random.nextInt(this.lockTextures.size()));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.frame++;

        SeedQueueProfiler.swap("wall");

        SeedQueueProfiler.push("update_previews");
        this.updatePreviews();

        SeedQueueProfiler.swap("background");
        if (this.background != null) {
            this.drawAnimatedTexture(this.background, matrices, 0, 0, this.width, this.height);
        } else {
            this.renderBackground(matrices);
        }

        SeedQueueProfiler.swap("render_main");
        for (int i = 0; i < this.layout.main.size(); i++) {
            this.renderInstance(this.mainPreviews[i], this.layout.main, i, matrices);
        }
        if (this.layout.locked != null && this.lockedPreviews != null) {
            SeedQueueProfiler.swap("render_locked");
            for (int i = 0; i < this.layout.locked.size(); i++) {
                this.renderInstance(i < this.lockedPreviews.size() ? this.lockedPreviews.get(i) : null, this.layout.locked, i, matrices);
            }
        }
        int i = 0;
        SeedQueueProfiler.swap("render_preparing");
        for (Layout.Group group : this.layout.preparing) {
            int offset = i;
            for (; i < group.size(); i++) {
                this.renderInstance(i < this.preparingPreviews.size() ? this.preparingPreviews.get(i) : null, group, i - offset, matrices);
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

        SeedQueueProfiler.swap("reset");
        this.resetViewport();
        this.loadPreviewSettings(this.settingsCache, 0);

        if (this.overlay != null) {
            SeedQueueProfiler.swap("overlay");
            this.drawAnimatedTexture(this.overlay, matrices, 0, 0, this.width, this.height);
        }

        if (this.debugHud != null) {
            SeedQueueProfiler.swap("fps_graph");
            ((DebugHudAccessor) this.debugHud).seedQueue$drawMetricsData(matrices, this.client.getMetricsData(), 0, this.width / 2, true);
        }
        SeedQueueProfiler.pop();
    }

    private void renderInstance(SeedQueuePreview instance, Layout.Group group, int index, MatrixStack matrices) {
        Layout.Pos pos = group.getPos(index);
        if (pos == null) {
            return;
        }

        SeedQueueProfiler.push("set_viewport");
        this.setViewport(pos);

        if (instance == null || (SeedQueue.config.waitForPreviewSetup && !instance.isRenderingReady())) {
            SeedQueueProfiler.swap("instance_background");
            this.renderInstanceBackground(group, index, matrices);
            if (instance != null) {
                SeedQueueProfiler.swap("build_chunks");
                instance.build();
            }
            SeedQueueProfiler.swap("reset_viewport");
            this.resetViewport();
            SeedQueueProfiler.pop();
            return;
        }

        SeedQueueProfiler.swap("load_settings");
        this.loadPreviewSettings(instance);

        SeedQueueProfiler.swap("render_preview");
        SeedQueueWallScreen.startRenderingPreview();
        instance.render(matrices);
        SeedQueueWallScreen.stopRenderingPreview();

        SeedQueueProfiler.swap("instance_overlay");
        this.renderInstanceOverlay(group, matrices);

        SeedQueueProfiler.swap("reset_viewport");
        this.resetViewport();

        if (instance.getSeedQueueEntry().isLocked()) {
            SeedQueueProfiler.swap("lock");
            this.drawLock(matrices, pos, instance.getLockTexture());
        }
        SeedQueueProfiler.pop();
    }

    private void renderInstanceBackground(Layout.Group group, int index, MatrixStack matrices) {
        if (!group.instanceBackground) {
            return;
        }
        if (!SeedQueue.config.waitForPreviewSetup && this.layout.main == group && !this.blockedMainPositions.contains(index)) {
            this.renderPreviewBackground();
            this.renderInstanceOverlay(group, matrices);
        } else if (this.instanceBackground != null) {
            this.drawAnimatedTexture(this.instanceBackground, matrices, 0, 0, this.width, this.height);
        }
    }

    private void renderPreviewBackground() {
        int scale = SeedQueue.config.calculateSimulatedScaleFactor(
                this.client.options.guiScale,
                this.client.options.forceUnicodeFont
        );
        int width = SeedQueue.config.simulatedWindowSize.width() / scale;
        int height = SeedQueue.config.simulatedWindowSize.height() / scale;

        this.setOrtho(width, height);
        SeedQueuePreview.renderBackground(width, height);
        this.resetOrtho();
    }

    private void renderInstanceOverlay(Layout.Group group, MatrixStack matrices) {
        if (group.instanceOverlay && this.instanceOverlay != null) {
            this.drawAnimatedTexture(this.instanceOverlay, matrices, 0, 0, this.width, this.height);
        }
    }

    private void drawLock(MatrixStack matrices, Layout.Pos pos, LockTexture lock) {
        this.setOrtho(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
        this.client.getTextureManager().bindTexture(lock.getId());

        int x = pos.x;
        int y = pos.y;
        int width = pos.width;
        int height = pos.height;

        if (lock.posX != 0 || lock.posY != 0 || lock.specifiedWidth != lock.width || lock.specifiedHeight != lock.height) {
            x += (int) (lock.posX * width);
            y += (int) (lock.posY * height);
            width = (int) (lock.specifiedWidth * width);
            height = (int) (lock.specifiedHeight * height);
        } else {
            width = Math.min(width, (int) (height * lock.getAspectRatio()));
            height = (int) (width / lock.getAspectRatio());
        }

        DrawableHelper.drawTexture(
                matrices,
                x,
                y,
                0.0f,
                lock.getFrameIndex(this.ticks) * lock.height,
                width,
                height,
                width,
                lock.height
        );
        this.resetOrtho();
    }

    @SuppressWarnings("SameParameterValue")
    private void drawAnimatedTexture(AnimatedTexture texture, MatrixStack matrices, int x, int y, int width, int height) {
        this.client.getTextureManager().bindTexture(texture.getId());
        RenderSystem.enableBlend();
        DrawableHelper.drawTexture(
                matrices,
                x,
                y,
                0.0f,
                texture.getFrameIndex(this.ticks) * height,
                width,
                height,
                width,
                height * texture.getIndividualFrameCount()
        );
        RenderSystem.disableBlend();
    }

    private boolean playSound(SoundEvent sound) {
        // spread out reset sounds over multiple ticks
        if (sound == SeedQueueSounds.RESET_INSTANCE) {
            if (this.nextResetSoundTick >= this.ticks) {
                return SeedQueueSounds.play(sound, ++this.nextResetSoundTick - this.ticks);
            }
            this.nextResetSoundTick = this.ticks;
        }
        return SeedQueueSounds.play(sound);
    }

    private void setViewport(Layout.Pos pos) {
        this.setViewport(pos.x, this.client.getWindow().getFramebufferHeight() - pos.height - pos.y, pos.width, pos.height);
        this.currentPos = pos;
    }

    private void setViewport(int x, int y, int width, int height) {
        RenderSystem.viewport(x, y, width, height);
    }

    protected void refreshViewport() {
        if (this.currentPos != null) {
            SeedQueueWallScreen.stopRenderingPreview();
            this.setViewport(this.currentPos);
            SeedQueueWallScreen.startRenderingPreview();
        }
    }

    private void resetViewport() {
        Window window = this.client.getWindow();
        this.setViewport(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight());
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

    protected void resetOrtho() {
        this.setOrtho(this.width, this.height);
    }

    private void updatePreviews() {
        this.updateLockedPreviews();
        this.updatePreparingPreviews();
        this.updateMainPreviews();
    }

    private void addLockedPreview(SeedQueuePreview preview) {
        Objects.requireNonNull(this.lockedPreviews).add(preview);
        preview.getSeedQueueEntry().mainPosition = -1;
    }

    private void updateLockedPreviews() {
        if (this.lockedPreviews == null) {
            return;
        }
        for (SeedQueueEntry entry : this.getAvailableSeedQueueEntries()) {
            if (entry.isLocked()) {
                this.addLockedPreview(new SeedQueuePreview(this, entry));
            }
        }
        for (int i = 0; i < this.mainPreviews.length; i++) {
            SeedQueuePreview instance = this.mainPreviews[i];
            if (instance != null && instance.getSeedQueueEntry().isLocked()) {
                this.addLockedPreview(instance);
                this.mainPreviews[i] = null;
                if (!this.layout.replaceLockedInstances) {
                    this.blockedMainPositions.add(i);
                }
            }
        }
        for (SeedQueuePreview instance : this.preparingPreviews) {
            if (instance.getSeedQueueEntry().isLocked()) {
                this.addLockedPreview(instance);
            }
        }
        this.preparingPreviews.removeAll(this.lockedPreviews);
    }

    private void updateMainPreviews() {
        int preparingCount = this.preparingPreviews.size();
        for (int i = 0; i < preparingCount; i++) {
            SeedQueuePreview preview = this.preparingPreviews.get(0);
            int position = preview.getSeedQueueEntry().mainPosition;

            if (position == -1) {
                break;
            }

            if (position >= this.mainPreviews.length) {
                preview.getSeedQueueEntry().mainPosition = -1;
                continue;
            }

            if (this.mainPreviews[position] != null) {
                SeedQueue.LOGGER.warn("Main preview {} already populated", position);
            } else {
                this.mainPreviews[position] = preview;
                this.preparingPreviews.remove(0);
            }
        }

        this.preparingPreviews.sort(Comparator.comparing(SeedQueuePreview::isRenderingReady, Comparator.reverseOrder()));

        IntArrayList previewsOrder = IntStream.range(0, this.mainPreviews.length).collect(IntArrayList::new, IntList::add, IntList::addAll);
        if (this.layout.mainFillOrder == Layout.MainFillOrder.RANDOM) {
            Collections.shuffle(previewsOrder);
        } else if (this.layout.mainFillOrder == Layout.MainFillOrder.BACKWARD) {
            Collections.reverse(previewsOrder);
        }

        for (int i : previewsOrder) {
            if (this.preparingPreviews.isEmpty() || SeedQueue.config.waitForPreviewSetup && !this.preparingPreviews.get(0).isRenderingReady()) {
                break;
            }

            if (this.mainPreviews[i] == null && !this.blockedMainPositions.contains(i)) {
                this.mainPreviews[i] = this.preparingPreviews.remove(0);
                this.mainPreviews[i].resetCooldown();

                if (this.mainPreviews[i].getSeedQueueEntry().mainPosition != -1) {
                    SeedQueue.LOGGER.warn("Main preview {} already assigned a position", i);
                } else {
                    this.mainPreviews[i].getSeedQueueEntry().mainPosition = i;
                }
            }
        }
    }

    private void updatePreparingPreviews() {
        int urgent = (int) Arrays.stream(this.mainPreviews).filter(Objects::isNull).count() - Math.min(this.blockedMainPositions.size(), this.preparingPreviews.size());
        int capacity = getBackgroundPreviews() + urgent;
        if (this.preparingPreviews.size() < capacity) {
            int budget = Math.max(1, urgent);

            // see SeedQueueWallScreen#updateMainPreviews
            // Previews which have previously been in the main group will have a position assigned
            // to them. They must be sorted to appear first in SeedQueueWallScreen#preparingPreviews
            // so that they can be restored to the correct location before any other previews take
            // their place.
            List<SeedQueueEntry> entries = this.getAvailableSeedQueueEntries();
            entries.sort(Comparator.comparing(entry -> entry.mainPosition, Comparator.reverseOrder()));

            for (SeedQueueEntry entry : entries) {
                this.preparingPreviews.add(new SeedQueuePreview(this, entry));
                if (--budget <= 0) {
                    break;
                }
            }
        } else {
            clearWorldRenderer(getClearableWorldRenderer());
        }
    }

    private int getBackgroundPreviews() {
        if (SeedQueue.config.preparingPreviews == -1) {
            int mainGroupSize = this.layout.main.size();
            int preparingGroupSize = Layout.Group.totalSize(this.layout.preparing);

            return Math.min(Math.max(mainGroupSize, preparingGroupSize), SeedQueue.config.maxCapacity - mainGroupSize);
        }
        return SeedQueue.config.preparingPreviews;
    }

    private List<SeedQueueEntry> getAvailableSeedQueueEntries() {
        List<SeedQueueEntry> entries = SeedQueue.getEntries();
        for (SeedQueuePreview instance : this.getInstances()) {
            entries.remove(instance.getSeedQueueEntry());
        }
        entries.removeIf(entry -> entry.getWorldGenerationProgressTracker() == null);
        if (SeedQueue.config.waitForPreviewSetup && !SeedQueue.config.isChunkmapResetting()) {
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
        if (settingsCache != this.lastSettingsCache) {
            settingsCache.loadPreview();
            this.lastSettingsCache = settingsCache;
        }
        this.client.options.perspective = perspective;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
        if (SeedQueueKeyBindings.scheduleAll.matchesMouse(button)) {
            this.scheduleAll();
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
        if (SeedQueueKeyBindings.scheduleJoin.matchesMouse(button)) {
            this.scheduleJoin(instance);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
            ModCompat.standardsettings$loadCache();
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
        if (SeedQueueKeyBindings.resetColumn.matchesKey(keyCode, scanCode)) {
            this.resetColumn(mouseX);
        }
        if (SeedQueueKeyBindings.resetRow.matchesKey(keyCode, scanCode)) {
            this.resetRow(mouseY);
        }
        if (SeedQueueKeyBindings.playNextLock.matchesKey(keyCode, scanCode)) {
            this.playNextLock();
        }
        if (SeedQueueKeyBindings.scheduleAll.matchesKey(keyCode, scanCode)) {
            this.scheduleAll();
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
        if (SeedQueueKeyBindings.scheduleJoin.matchesKey(keyCode, scanCode)) {
            this.scheduleJoin(instance);
        }
        return true;
    }

    private SeedQueuePreview getInstance(double mouseX, double mouseY) {
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
        if (instance.hasRendered() && this.canPlayInstance(instance.getSeedQueueEntry())) {
            if (this.removePreview(instance)) {
                this.playEntry(instance.getSeedQueueEntry());
                return;
            }
        } else {
            this.lockInstance(instance);
        }

        if (SeedQueue.config.smartSwitch) {
            this.playNextLock();
        }
    }

    private void playInstance(SeedQueueEntry entry) {
        if (this.canPlayInstance(entry)) {
            this.removePreview(this.getPreview(entry));
            this.playEntry(entry);
        }
    }

    private void playEntry(SeedQueueEntry entry) {
        this.playSound(SeedQueueSounds.PLAY_INSTANCE);
        SeedQueue.playEntry(entry);
    }

    private boolean canPlayInstance(SeedQueueEntry entry) {
        return this.client.currentScreen == this && !this.client.isIntegratedServerRunning() && entry.isReady();
    }

    private void lockInstance(SeedQueuePreview instance) {
        if (instance.hasRendered() && instance.getSeedQueueEntry().lock()) {
            if (this.lockedPreviews != null) {
                int index;
                if (!this.layout.replaceLockedInstances && (index = ArrayUtils.indexOf(this.mainPreviews, instance)) != -1) {
                    this.blockedMainPositions.add(index);
                }
                if (this.removePreview(instance)) {
                    this.addLockedPreview(instance);
                }
            }
            if (SeedQueue.config.freezeLockedPreviews) {
                // clearing SeedQueuePreviewProperties frees the previews WorldRenderer, allowing resources to be cleared
                // it also means the amount of WorldRenderers does not exceed Rows * Columns + Background Previews
                // when a custom layout with a locked group is used
                instance.getSeedQueueEntry().setPreviewProperties(null);
            }
            this.playSound(SeedQueueSounds.LOCK_INSTANCE);
        }
    }

    private boolean resetInstance(SeedQueuePreview instance, boolean ignoreLock, boolean ignoreResetCooldown, boolean playSound) {
        if (instance == null || !instance.canReset(ignoreLock, ignoreResetCooldown) || !this.removePreview(instance)) {
            return false;
        }

        SeedQueueProfiler.push("reset_instance");
        SeedQueue.discard(instance.getSeedQueueEntry());

        this.scheduledEntries.remove(instance.getSeedQueueEntry());

        if (playSound) {
            this.playSound(SeedQueueSounds.RESET_INSTANCE);
        }
        this.showFinishedBenchmarkResults = false;

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

    private boolean removePreview(SeedQueuePreview preview) {
        if (preview == null) {
            return false;
        }
        boolean removed = false;
        for (int i = 0; i < this.mainPreviews.length; i++) {
            if (this.mainPreviews[i] == preview) {
                this.mainPreviews[i] = null;
                removed = true;
            }
        }
        removed |= this.preparingPreviews.remove(preview);
        if (this.lockedPreviews != null) {
            removed |= this.lockedPreviews.remove(preview);
        }
        return removed;
    }

    private void resetAllInstances() {
        boolean playSound = !this.playSound(SeedQueueSounds.RESET_ALL);
        for (SeedQueuePreview instance : this.mainPreviews) {
            this.resetInstance(instance, false, false, playSound);
        }
        this.blockedMainPositions.clear();
    }

    private void focusReset(SeedQueuePreview instance) {
        this.playInstance(instance);
        this.resetAllInstances();
    }

    private void resetColumn(double mouseX) {
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
        SeedQueue.getEntryMatching(entry -> entry.isLocked() && entry.isReady()).ifPresent(this::playInstance);
    }

    private void scheduleAll() {
        this.playSound(SeedQueueSounds.SCHEDULE_ALL);
        for (SeedQueueEntry entry : SeedQueue.getEntries()) {
            if (!entry.isLocked()) {
                continue;
            }
            this.scheduleJoin(entry);
            if (entry.isLoaded()) {
                break;
            }
        }
    }

    private void scheduleJoin(SeedQueuePreview instance) {
        if (instance.hasRendered()) {
            this.lockInstance(instance);
            if (this.scheduleJoin(instance.getSeedQueueEntry())) {
                this.playSound(SeedQueueSounds.SCHEDULE_JOIN);
            }
        }
    }

    private boolean scheduleJoin(SeedQueueEntry entry) {
        if (!entry.isLocked()) {
            SeedQueue.LOGGER.warn("Tried to schedule join but entry isn't locked!");
            return false;
        }
        if (this.canPlayInstance(entry)) {
            this.playInstance(entry);
            return false;
        }
        return this.scheduledEntries.add(entry);
    }

    public void joinScheduledInstance() {
        // catch the case were someone resets a scheduled entry after it has played the warning sound
        this.playedScheduledEnterWarning &= !this.scheduledEntries.isEmpty();

        for (SeedQueueEntry entry : this.scheduledEntries) {
            // 95% world gen percentage should be made configurable in the future
            // not sure how, maybe through .mcmeta
            if (!this.playedScheduledEnterWarning && entry.getProgressPercentage() >= 95) {
                this.playSound(SeedQueueSounds.SCHEDULED_JOIN_WARNING);
                this.playedScheduledEnterWarning = true;
            }
            if (entry.isReady()) {
                this.playInstance(entry);
                break;
            }
        }
    }

    @Override
    public void tick() {
        this.ticks++;
    }

    public void populateResetCooldowns() {
        long cooldownStart = Util.getMeasuringTimeMs();
        for (SeedQueuePreview instance : this.getInstances()) {
            instance.populateCooldownStart(cooldownStart);
        }
    }

    private void startBenchmark() {
        this.clearSeedQueueForBenchmark();
        this.benchmarkGoal = SeedQueue.config.benchmarkResets;
        this.benchmarkStart = Util.getMeasuringTimeMs();
        this.benchmarkedSeeds = 0;
        SeedQueue.LOGGER.info("BENCHMARK | Starting benchmark with a goal of {} resets.", this.benchmarkGoal);
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

    private void clearSeedQueueForBenchmark() {
        // clearing queue is synchronized with world creation
        // to avoid any worlds named Benchmark Reset #xxx to be able to be played
        synchronized (SeedQueueThread.WORLD_CREATION_LOCK) {
            for (SeedQueueEntry entry : SeedQueue.getEntries()) {
                SeedQueue.discard(entry);
            }
            for (SeedQueuePreview instance : this.getInstances()) {
                this.removePreview(instance);
            }
        }
    }

    private void finishBenchmark() {
        this.benchmarkFinish = Util.getMeasuringTimeMs();
        SeedQueue.LOGGER.info("BENCHMARK | Reset {} seeds in {} seconds.", this.benchmarkedSeeds, Math.round((this.benchmarkFinish - this.benchmarkStart) / 10.0) / 100.0);
        this.playSound(SeedQueueSounds.FINISH_BENCHMARK);
        this.showFinishedBenchmarkResults = true;

        // any worlds named Benchmark Reset #xxx are cleared after benchmark finishes
        this.clearSeedQueueForBenchmark();
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
                    break;
                }
                if (this.benchmarkedSeeds % 100 == 0) {
                    SeedQueue.LOGGER.info("BENCHMARK | Reset {} seeds in {} seconds...", this.benchmarkedSeeds, Math.round((Util.getMeasuringTimeMs() - this.benchmarkStart) / 10.0) / 100.0);
                }
            }
        }
    }

    public boolean isBenchmarking() {
        return this.benchmarkedSeeds < this.benchmarkGoal;
    }

    @Override
    public void removed() {
        this.client.getToastManager().clear();
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
            if (!SeedQueue.hasEntryMatching(entry -> {
                SeedQueuePreviewProperties previewProperties = entry.getPreviewProperties();
                return previewProperties != null && previewProperties.world == worldRendererWorld;
            })) {
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

    private static void startRenderingPreview() {
        renderingPreview = true;
    }

    private static void stopRenderingPreview() {
        renderingPreview = false;
    }

    public static boolean shouldModifyWindowSize() {
        return renderingPreview;
    }
}
