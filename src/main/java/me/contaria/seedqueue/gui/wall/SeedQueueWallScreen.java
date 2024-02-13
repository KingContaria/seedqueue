package me.contaria.seedqueue.gui.wall;

import com.mojang.blaze3d.systems.RenderSystem;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
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

    private final int rows;
    private final int columns;

    protected int frame;

    private final long benchmarkStart = System.currentTimeMillis();
    private int benchmarkedSeeds;

    private long pressedResetAll;

    public SeedQueueWallScreen(Screen createWorldScreen, int rows, int columns) {
        super(LiteralText.EMPTY);
        this.createWorldScreen = createWorldScreen;
        this.rows = rows;
        this.columns = columns;
        this.loadingScreens = new SeedQueueLevelLoadingScreen[rows * columns];
        this.backupLoadingScreens = new ArrayList<>(SeedQueue.config.backgroundPreviews);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.frame++;
        this.debugResetAll("New frame");

        this.updatePreviews();
        this.debugResetAll("Updated Previews");
        Set<SeedQueueLevelLoadingScreen> instancesToRender = Arrays.stream(this.loadingScreens).filter(Objects::nonNull).collect(Collectors.toSet());
        if (instancesToRender.isEmpty()) {
            this.renderBackground(matrices);
        }
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
                    if (!instance.hasBeenRendered() && !instance.shouldRender()) {
                        instance.buildChunks();
                        instance.renderBackground(matrices);
                        continue;
                    }
                    if (!instancesToRender.contains(instance)) {
                        if (!instance.hasBeenRendered()) {
                            instance.renderBackground(matrices);
                        } else if (SeedQueue.config.alwaysRenderChunkMap) {
                            instance.renderChunkMap(matrices);
                        }
                        instance.buildChunks();
                    } else {
                        instance.render(matrices, 0, 0, delta);
                    }
                }
            }
        } finally {
            RenderSystem.viewport(0, 0, this.client.getWindow().getWidth(), this.client.getWindow().getHeight());
        }
        this.debugResetAll("Rendered Previews");

        for (SeedQueueLevelLoadingScreen backupInstance : this.backupLoadingScreens) {
            backupInstance.buildChunks();
        }
        this.debugResetAll("Built Background Previews");
        this.debugResetAll("Done");
        this.pressedResetAll = 0;
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
        return true;
    }

    private void resetAllInstances() {
        if (SeedQueue.config.timeResetAll) {
            this.pressedResetAll = System.currentTimeMillis();
        }
        this.debugResetAll("Pressed");
        for (SeedQueueLevelLoadingScreen instance : this.loadingScreens) {
            this.resetInstance(instance);
        }
        this.debugResetAll("Instances are reset");
    }

    private void debugResetAll(String action) {
        if (this.pressedResetAll != 0) {
            SeedQueue.LOGGER.info("DEBUG | Reset All - {} at {}ms.", action, System.currentTimeMillis() - this.pressedResetAll);
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
        List<SeedQueueEntry> availableSeedQueueEntries = new ArrayList<>(SeedQueue.SEED_QUEUE);
        availableSeedQueueEntries.removeAll(Arrays.stream(this.loadingScreens).filter(Objects::nonNull).map(SeedQueueLevelLoadingScreen::getSeedQueueEntry).collect(Collectors.toList()));
        availableSeedQueueEntries.removeAll(this.backupLoadingScreens.stream().map(SeedQueueLevelLoadingScreen::getSeedQueueEntry).collect(Collectors.toList()));
        availableSeedQueueEntries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldGenerationProgressTracker() == null);
        availableSeedQueueEntries.removeIf(seedQueueEntry -> seedQueueEntry.getWorldPreviewProperties() == null);

        int previewsSetup = 0;
        int previewSetupLimit = SeedQueue.config.previewSetupBuffer > 0 ? SeedQueue.config.previewSetupBuffer : Integer.MAX_VALUE;
        for (int i = 0; i < this.loadingScreens.length && !(this.backupLoadingScreens.isEmpty() && availableSeedQueueEntries.isEmpty()); i++) {
            if (this.loadingScreens[i] == null) {
                if (!this.backupLoadingScreens.isEmpty()) {
                    this.loadingScreens[i] = this.backupLoadingScreens.remove(0);
                    continue;
                }
                this.loadingScreens[i] = new SeedQueueLevelLoadingScreen(this, availableSeedQueueEntries.remove(0));
                previewsSetup++;

                if (previewsSetup >= previewSetupLimit) {
                    return;
                }
            }
        }

        if (SeedQueue.config.renderPreviewsBeforeBackgroundSetup && Arrays.stream(this.loadingScreens).anyMatch(instance -> instance != null && !instance.hasBeenRendered())) {
            return;
        }

        previewSetupLimit = SeedQueue.config.backgroundPreviewSetupBuffer > 0 ? SeedQueue.config.backgroundPreviewSetupBuffer : previewSetupLimit;
        while (this.backupLoadingScreens.size() < SeedQueue.config.backgroundPreviews && !availableSeedQueueEntries.isEmpty()) {
            this.backupLoadingScreens.add(new SeedQueueLevelLoadingScreen(this, availableSeedQueueEntries.remove(0)));
            previewsSetup++;

            if (previewsSetup >= previewSetupLimit) {
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

    static WorldRenderer getWorldRenderer(ClientWorld world) {
        WorldRenderer unusedWorldRenderer = null;
        for (WorldRenderer worldRenderer : WORLD_RENDERERS) {
            ClientWorld worldRendererWorld = ((WorldRendererAccessor) worldRenderer).seedQueue$getWorld();
            if (worldRendererWorld == world) {
                return worldRenderer;
            }
            if (worldRendererWorld == null || !SeedQueue.getEntryMatching(entry -> {
                WorldPreviewProperties worldPreviewProperties = entry.getWorldPreviewProperties();
                if (worldPreviewProperties == null) {
                    return false;
                }
                return worldRendererWorld == worldPreviewProperties.getWorld();
            }).isPresent()) {
                unusedWorldRenderer = worldRenderer;
            }
        }
        WorldRenderer worldRenderer = unusedWorldRenderer;
        if (worldRenderer == null) {
            worldRenderer = new WorldRenderer(MinecraftClient.getInstance(), new BufferBuilderStorage());
            WORLD_RENDERERS.add(worldRenderer);
        }
        worldRenderer.setWorld(world);
        return worldRenderer;
    }

    public static void clearWorldRenderer(ClientWorld world) {
        for (WorldRenderer worldRenderer : WORLD_RENDERERS) {
            ClientWorld worldRendererWorld = ((WorldRendererAccessor) worldRenderer).seedQueue$getWorld();
            if (worldRendererWorld == world) {
                worldRenderer.setWorld(null);
            }
        }
    }
}
