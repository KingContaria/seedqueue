package me.contaria.seedqueue.gui.wall;

import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.SeedQueueSettingsCache;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.keybindings.SeedQueueKeyBindings;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

public class SeedQueueWallScreen extends Screen {

    private static final Set<WorldRenderer> WORLD_RENDERERS = new HashSet<>();

    private final SeedQueueLevelLoadingScreen[] loadingScreens;
    private final List<SeedQueueLevelLoadingScreen> backupLoadingScreens;
    private final Screen createWorldScreen;

    protected final SeedQueueSettingsCache settingsCache;
    private SeedQueueSettingsCache lastSettingsCache;

    private final int rows;
    private final int columns;

    private boolean shouldRenderBackground;
    protected int frame;

    private final long benchmarkStart = System.currentTimeMillis();
    private int benchmarkedSeeds;

    public SeedQueueWallScreen(Screen createWorldScreen, int rows, int columns) {
        super(LiteralText.EMPTY);
        this.createWorldScreen = createWorldScreen;
        this.rows = rows;
        this.columns = columns;
        this.loadingScreens = new SeedQueueLevelLoadingScreen[rows * columns];
        this.backupLoadingScreens = new ArrayList<>(SeedQueue.config.backgroundPreviews);
        this.lastSettingsCache = this.settingsCache = SeedQueueSettingsCache.create();
    }

    @Override
    protected void init() {
        this.shouldRenderBackground = true;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.frame++;

        this.updatePreviews();

        if (this.shouldRenderBackground) {
            this.renderBackground(matrices);
            this.shouldRenderBackground = false;
        }

        Set<SeedQueueLevelLoadingScreen> instancesToRender = Arrays.stream(this.loadingScreens).filter(Objects::nonNull).collect(Collectors.toSet());
        if (SeedQueue.config.previewRenderLimit > 0) {
            instancesToRender = instancesToRender.stream().filter(instance -> instance.hasBeenRendered() || instance.shouldRender()).sorted(Comparator.comparingInt(e1 -> e1.lastRenderFrame)).limit(SeedQueue.config.previewRenderLimit).collect(Collectors.toSet());
        }

        assert this.client != null;
        int width = this.getInstanceWidth();
        int height = this.getInstanceHeight();

        try {
            for (int row = 0; row < this.rows; row++) {
                for (int column = 0; column < this.columns; column++) {
                    int x = column * width;
                    int y = (this.rows - row - 1) * height;
                    RenderSystem.viewport(x, y, width, height);

                    SeedQueueLevelLoadingScreen instance = this.getInstance(row, column);

                    if (instance == null) {
                        this.renderBackground(matrices);
                        continue;
                    }

                    this.loadPreviewSettings(instance.getWorldPreviewProperties().getSettingsCache(), instance.getWorldPreviewProperties().getPerspective());

                    if (!instance.hasBeenRendered() && !instance.shouldRender()) {
                        instance.buildChunks();
                        instance.renderBackground(matrices);
                        continue;
                    }
                    if (!instancesToRender.contains(instance)) {
                        if (!instance.hasBeenRendered()) {
                            instance.renderBackground(matrices);
                        } else {
                            if (SeedQueue.config.alwaysRenderChunkMap) {
                                instance.renderChunkMap(matrices);
                            }
                            if (instance.getSeedQueueEntry().isLocked() && !instance.hasRenderedLock()) {
                                instance.renderLock(matrices);
                            }
                        }
                        instance.buildChunks();
                        continue;
                    }
                    instance.render(matrices, 0, 0, delta);
                }
            }
        } finally {
            RenderSystem.viewport(0, 0, this.client.getWindow().getWidth(), this.client.getWindow().getHeight());
        }

        for (SeedQueueLevelLoadingScreen backupInstance : this.backupLoadingScreens) {
            this.loadPreviewSettings(backupInstance.getWorldPreviewProperties().getSettingsCache(), backupInstance.getWorldPreviewProperties().getPerspective());
            backupInstance.buildChunks();
        }

        this.loadPreviewSettings(this.settingsCache, 0);
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
        if (SeedQueueKeyBindings.resetRow.matchesMouse(button)) {
            this.resetRow(this.getRow(mouseY));
        }
        if (SeedQueueKeyBindings.resetColumn.matchesMouse(button)) {
            this.resetColumn(this.getColumn(mouseX));
        }

        SeedQueueLevelLoadingScreen instance = this.getInstance(mouseX, mouseY);
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

        if (SeedQueueKeyBindings.resetAll.matchesKey(keyCode, scanCode)) {
            this.resetAllInstances();
        }
        if (SeedQueueKeyBindings.resetRow.matchesKey(keyCode, scanCode)) {
            this.resetRow(this.getRow(mouseY));
        }
        if (SeedQueueKeyBindings.resetColumn.matchesKey(keyCode, scanCode)) {
            this.resetColumn(this.getColumn(mouseX));
        }

        SeedQueueLevelLoadingScreen instance = this.getInstance(mouseX, mouseY);
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

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        super.resize(client, width, height);
        for (SeedQueueLevelLoadingScreen preview : this.loadingScreens) {
            if (preview != null) {
                preview.resize(client, width, height);
            }
        }
    }

    public int getInstanceWidth() {
        assert this.client != null;
        return this.client.getWindow().getWidth() / this.columns;
    }

    public int getInstanceHeight() {
        assert this.client != null;
        return this.client.getWindow().getHeight() / this.rows;
    }

    private SeedQueueLevelLoadingScreen getInstance(int row, int column) {
        int index = row * this.columns + column;
        if (index < 0 || index >= this.loadingScreens.length) {
            return null;
        }
        return this.loadingScreens[row * this.columns + column];
    }

    private SeedQueueLevelLoadingScreen getInstance(double mouseX, double mouseY) {
        return this.getInstance(this.getRow(mouseY), this.getColumn(mouseX));
    }

    private int getRow(double mouseY) {
        return (int) (mouseY / (this.height / this.rows));
    }

    private int getColumn(double mouseX) {
        return (int) (mouseX / (this.width / this.columns));
    }

    private boolean playInstance(SeedQueueLevelLoadingScreen instance) {
        assert this.client != null;
        SeedQueueEntry seedQueueEntry = instance.getSeedQueueEntry();
        if (!instance.hasBeenRendered() || !seedQueueEntry.isReady() || SeedQueue.selectedEntry != null) {
            return false;
        }
        SeedQueue.selectedEntry = seedQueueEntry;
        if (!SeedQueue.config.lazilyClearWorldRenderers) {
            clearWorldRenderer(getWorldRenderer(instance.getWorldPreviewProperties().getWorld()));
        }
        this.client.openScreen(this.createWorldScreen);
        return true;
    }

    private void lockInstance(SeedQueueLevelLoadingScreen instance) {
        instance.getSeedQueueEntry().lock();
    }

    private boolean resetInstance(SeedQueueLevelLoadingScreen instance) {
        return this.resetInstance(instance, false);
    }

    private boolean resetInstance(SeedQueueLevelLoadingScreen instance, boolean ignoreLock) {
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

        for (int i = 0; i < this.loadingScreens.length; i++) {
            if (this.loadingScreens[i] == instance) {
                this.loadingScreens[i] = null;
            }
        }
        if (!SeedQueue.config.lazilyClearWorldRenderers) {
            clearWorldRenderer(getWorldRenderer(instance.getWorldPreviewProperties().getWorld()));
        }
        return true;
    }

    private void resetAllInstances() {
        for (SeedQueueLevelLoadingScreen instance : this.loadingScreens) {
            this.resetInstance(instance);
        }
    }

    private void resetRow(int row) {
        for (int column = 0; column < this.columns; column++) {
            this.resetInstance(this.getInstance(row, column));
        }
    }

    private void resetColumn(int column) {
        for (int row = 0; row < this.rows; row++) {
            this.resetInstance(this.getInstance(row, column));
        }
    }

    private void updatePreviews() {
        List<SeedQueueLevelLoadingScreen> readyInstances = this.backupLoadingScreens.stream().filter(SeedQueueLevelLoadingScreen::shouldRender).collect(Collectors.toList());
        for (int i = 0; i < this.loadingScreens.length && !readyInstances.isEmpty(); i++) {
            if (this.loadingScreens[i] == null) {
                this.backupLoadingScreens.remove(this.loadingScreens[i] = readyInstances.remove(0));
            }
        }

        List<SeedQueueEntry> availableSeedQueueEntries = new ArrayList<>(SeedQueue.SEED_QUEUE);
        availableSeedQueueEntries.removeAll(Arrays.stream(this.loadingScreens).filter(Objects::nonNull).map(SeedQueueLevelLoadingScreen::getSeedQueueEntry).collect(Collectors.toList()));
        availableSeedQueueEntries.removeAll(this.backupLoadingScreens.stream().map(SeedQueueLevelLoadingScreen::getSeedQueueEntry).collect(Collectors.toList()));
        availableSeedQueueEntries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldGenerationProgressTracker() == null);
        availableSeedQueueEntries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldPreviewProperties() == null);

        int previewsSetup = 0;
        int backgroundCapacity = SeedQueue.config.backgroundPreviews + (int) Arrays.stream(this.loadingScreens).filter(Objects::isNull).count();
        for (SeedQueueEntry entry : availableSeedQueueEntries) {
            if (this.backupLoadingScreens.size() >= backgroundCapacity) {
                break;
            }
            this.backupLoadingScreens.add(new SeedQueueLevelLoadingScreen(this, entry));
            previewsSetup++;

            if (previewsSetup >= SeedQueue.config.previewSetupBuffer) {
                return;
            }
        }

        WorldRenderer worldRendererToClear;
        while ((worldRendererToClear = getClearableWorldRenderer()) != null) {
            worldRendererToClear.setWorld(null);
            previewsSetup++;

            if (previewsSetup >= SeedQueue.config.previewSetupBuffer) {
                return;
            }
        }
    }

    public void tickBenchmark() {
        if (!this.isBenchmarking()) {
            return;
        }
        for (SeedQueueLevelLoadingScreen instance : this.loadingScreens) {
            if (this.resetInstance(instance, true)) {
                this.benchmarkedSeeds++;
                if (this.benchmarkedSeeds == SeedQueue.config.benchmarkResets) {
                    SeedQueue.LOGGER.info("BENCHMARK | Reset {} seeds in {} seconds.", this.benchmarkedSeeds, (System.currentTimeMillis() - this.benchmarkStart) / 1000.0);
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
}
