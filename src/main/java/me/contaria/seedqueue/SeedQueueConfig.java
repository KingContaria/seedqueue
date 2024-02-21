package me.contaria.seedqueue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.gui.config.SeedQueueKeybindingsScreen;
import me.contaria.seedqueue.keybindings.SeedQueueKeyBindings;
import me.contaria.seedqueue.keybindings.SeedQueueMultiKeyBinding;
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

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(max = 100)
    public int maxWorldGenerationPercentage = 100;

    @Config.Category("chunkmap")
    public ChunkMapVisibility chunkMapVisibility = ChunkMapVisibility.TRUE;

    @Config.Category("chunkmap")
    @Config.Numbers.Whole.Bounds(min = 1, max = 5)
    public int chunkMapScale = 2;

    @Config.Ignored
    public boolean canUseWall = ModCompat.HAS_WORLDPREVIEW;

    @Config.Category("wall")
    public boolean useWall = false;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int rows = 2;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
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

    // this option is for removal and should just always be rows x columns
    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int previewSetupBuffer = 0;

    // this option is for removal and should just always be 1
    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int backgroundPreviewSetupBuffer = 0;

    @Config.Category("performance")
    public boolean renderPreviewsBeforeBackgroundSetup = true;

    // this option is for removal and should just always be true,
    // before that I want to implement a system that if no background preview is set up in a frame, it clears one worldrenderer instead
    @Config.Category("performance")
    public boolean lazilyClearWorldRenderers = false;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int previewRenderLimit = 0;

    // this option might be removed in the future and just always set to true,
    // it shouldn't hurt performance in any meaningful way and would make the use of previewRenderLimit feel much smoother
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
    @Config.Numbers.Whole.Bounds(min = 0, max = 31, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int backgroundExecutorThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    public int backgroundExecutorThreadPriority = Thread.NORM_PRIORITY;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 1, max = 31, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int wallExecutorThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    public int wallExecutorThreadPriority = Thread.NORM_PRIORITY;

    @Config.Category("debug")
    public boolean useWatchdog = false;

    @Config.Category("debug")
    public boolean doNotWaitForChunksToBuild = false;

    @Config.Category("debug")
    @Config.Numbers.Whole.Bounds(max = Integer.MAX_VALUE)
    @Config.Numbers.TextField
    public int benchmarkResets = 0;

    @Config.Category("misc")
    @Config.Name("seedqueue.menu.keys")
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
    public void setBackgroundExecutorThreads(int backgroundExecutorThreads) {
        this.backgroundExecutorThreads = Math.min(Runtime.getRuntime().availableProcessors(), backgroundExecutorThreads);
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
}
