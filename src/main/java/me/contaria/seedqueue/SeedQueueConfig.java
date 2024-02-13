package me.contaria.seedqueue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.contaria.seedqueue.gui.config.SeedQueueKeybindingsScreen;
import me.contaria.seedqueue.keybindings.SeedQueueKeyBindings;
import me.contaria.seedqueue.keybindings.SeedQueueMultiKeyBinding;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
import org.mcsr.speedrunapi.config.api.SpeedrunConfig;
import org.mcsr.speedrunapi.config.api.SpeedrunOption;
import org.mcsr.speedrunapi.config.api.annotations.Config;
import org.mcsr.speedrunapi.config.api.annotations.InitializeOn;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@InitializeOn(InitializeOn.InitPoint.PRELAUNCH)
public class SeedQueueConfig implements SpeedrunConfig {

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int maxCapacity = 0;

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = 1, max = 20, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int maxConcurrently = 1;

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = 1, max = 20, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int maxConcurrently_onWall = 1;

    @Config.Category("chunkmap")
    public ChunkMapVisibility chunkMapVisibility = ChunkMapVisibility.TRUE;

    @Config.Category("chunkmap")
    @Config.Numbers.Whole.Bounds(min = 1, max = 5)
    public int chunkMapScale = 2;

    @Config.Ignored
    public boolean canUseWall = FabricLoader.getInstance().isModLoaded("worldpreview");

    @Config.Category("wall")
    public boolean useWall = false;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Description.None
    public int rows = 2;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Description.None
    public int columns = 2;
/*
    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 0, max = Integer.MAX_VALUE, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Numbers.TextField
    public int simulatedWindowWidth;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 0, max = Integer.MAX_VALUE, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Numbers.TextField
    public int simulatedWindowHeight;

 */

    @Config.Category("wall")
    public boolean bypassWall = false;

    @Config.Category("performance")
    public boolean unlimitedWallFPS = false;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 8, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Access(setter = "setChunkUpdateThreads")
    public int chunkUpdateThreads = 0;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int previewSetupBuffer = 0;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int backgroundPreviewSetupBuffer = 0;

    @Config.Category("performance")
    public boolean renderPreviewsBeforeBackgroundSetup = true;

    @Config.Category("performance")
    public boolean lazilyClearWorldRenderers = false;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int previewRenderLimit = 0;

    @Config.Category("performance")
    public boolean alwaysRenderChunkMap = false;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int backgroundPreviews = 0;

    @Config.Category("performance")
    public boolean usePerThreadSurfaceBuilders = false;

    @Config.Category("performance")
    public boolean lazyUserCache = false;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 7, max = 31, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Access(setter = "setMaxServerExecutorThreads")
    public int maxServerExecutorThreads = 7;

    @Config.Category("executors")
    public BackgroundExecutorMode backgroundExecutorMode = BackgroundExecutorMode.OFF;

    @Config.Category("executors")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    @Config.Description.None
    public int executorPriority_background = Thread.MIN_PRIORITY;

    @Config.Category("executors")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    @Config.Description.None
    public int executorPriority_beforePreview = Thread.NORM_PRIORITY;

    @Config.Category("executors")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    @Config.Description.None
    public int executorPriority_afterPreview = Thread.MIN_PRIORITY;

    @Config.Category("executors")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    @Config.Description.None
    public int executorPriority_locked = Thread.NORM_PRIORITY;

    @Config.Category("debug")
    public boolean useWatchdog = false;

    @Config.Category("debug")
    @Config.Description.None
    public boolean doNotWaitForChunksToBuild = false;

    @Config.Category("debug")
    @Config.Numbers.Whole.Bounds(max = Integer.MAX_VALUE)
    @Config.Numbers.TextField
    public int benchmarkResets = 0;

    @Config.Category("debug")
    @Config.Description.None
    public boolean timeResetAll = false;

    @Config.Category("misc")
    @Config.Name("seedqueue.menu.keys")
    @Config.Description.None
    public final SeedQueueMultiKeyBinding[] keyBindings = new SeedQueueMultiKeyBinding[]{
            SeedQueueKeyBindings.play,
            SeedQueueKeyBindings.focusReset,
            SeedQueueKeyBindings.reset,
            SeedQueueKeyBindings.lock,
            SeedQueueKeyBindings.resetAll,
            SeedQueueKeyBindings.resetRow,
            SeedQueueKeyBindings.resetColumn
    };

    {
        SeedQueue.config = this;
    }

    @SuppressWarnings("unused")
    public void setChunkUpdateThreads(int chunkUpdateThreads) {
        this.chunkUpdateThreads = Math.min(Runtime.getRuntime().availableProcessors(), chunkUpdateThreads);
    }

    @SuppressWarnings("unused")
    public void setMaxServerExecutorThreads(int maxServerExecutorThreads) {
        this.maxServerExecutorThreads = Math.max(7, Math.min(Runtime.getRuntime().availableProcessors() - 1, maxServerExecutorThreads));
    }

    public boolean shouldUseWall() {
        return this.canUseWall && this.maxCapacity > 0 && this.useWall;
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        if (SeedQueueMultiKeyBinding[].class.equals(field.getType())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<SeedQueueMultiKeyBinding[]>(config, this, field, idPrefix)
                    .fromJson(((option, config_, configStorage, optionField, jsonElement) -> {
                        for (SeedQueueMultiKeyBinding keyBinding : option.get()) {
                            List<InputUtil.Key> keys = new ArrayList<>();
                            JsonElement keyJsonElement = jsonElement.getAsJsonObject().get(keyBinding.getTranslationKey());
                            if (keyJsonElement == null) {
                                continue;
                            }
                            for (JsonElement key : keyJsonElement.getAsJsonArray()) {
                                keys.add(InputUtil.fromTranslationKey(key.getAsString()));
                            }
                            keyBinding.setKeys(keys);
                        }
                    }))
                    .toJson(((option, config_, configStorage, optionField) -> {
                        JsonObject jsonObject = new JsonObject();
                        for (SeedQueueMultiKeyBinding keyBinding : option.get()) {
                            JsonArray jsonArray = new JsonArray();
                            for (InputUtil.Key key : keyBinding.getKeys()) {
                                jsonArray.add(new JsonPrimitive(key.getTranslationKey()));
                            }
                            jsonObject.add(keyBinding.getTranslationKey(), jsonArray);
                        }
                        return jsonObject;
                    }))
                    .setter((option, config_, configStorage, optionField, value) -> {
                        throw new UnsupportedOperationException();
                    })
                    .createWidget((option, config_, configStorage, optionField) -> new ButtonWidget(0, 0, 150, 20, new TranslatableText("seedqueue.menu.keys.configure"), button -> MinecraftClient.getInstance().openScreen(new SeedQueueKeybindingsScreen(MinecraftClient.getInstance().currentScreen, this.keyBindings))))
                    .build();
        }
        return SpeedrunConfig.super.parseField(field, config, idPrefix);
    }

    @Override
    public String modID() {
        return "seedqueue";
    }

    @Override
    public boolean isAvailable() {
        return !SeedQueue.isActive();
    }

    public enum ChunkMapVisibility {
        TRUE,
        TRANSPARENT,
        FALSE
    }

    public enum BackgroundExecutorMode {
        OFF,
        SINGLE_EXECUTOR,
        MULTI_EXECUTORS
    }
}
