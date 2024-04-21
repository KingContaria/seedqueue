package me.contaria.seedqueue;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.gui.config.SeedQueueKeybindingsScreen;
import me.contaria.seedqueue.keybindings.SeedQueueKeyBindings;
import me.contaria.seedqueue.keybindings.SeedQueueMultiKeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
import org.mcsr.speedrunapi.config.api.SpeedrunConfig;
import org.mcsr.speedrunapi.config.api.SpeedrunOption;
import org.mcsr.speedrunapi.config.api.annotations.Config;
import org.mcsr.speedrunapi.config.api.annotations.InitializeOn;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@InitializeOn(InitializeOn.InitPoint.PRELAUNCH)
public class SeedQueueConfig implements SpeedrunConfig {

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int maxCapacity = 0;

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = 0, max = 20, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
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
    public boolean canUseWall = ModCompat.HAS_WORLDPREVIEW && ModCompat.HAS_STANDARDSETTINGS;

    @Config.Category("wall")
    public boolean useWall = false;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int rows = 2;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int columns = 2;

    @Config.Category("wall")
    public JsonObject customLayout = null;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 0, max = 16384, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Numbers.TextField
    public int simulatedWindowWidth;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 0, max = 16384, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Numbers.TextField
    public int simulatedWindowHeight;

    @Config.Category("wall")
    public boolean replaceLockedPreviews = true;

    @Config.Category("wall")
    public boolean bypassWall = false;

    @Config.Category("performance")
    public boolean unlimitedWallFPS = false;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 0, max = 50, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int backgroundPreviews = 0;

    @Config.Category("performance")
    public boolean usePerThreadSurfaceBuilders = false;

    @Config.Category("performance")
    public boolean lazyUserCache = false;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    public int seedQueueThreadPriority = Thread.NORM_PRIORITY;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = 0, max = 31, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int backgroundExecutorThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    public int backgroundExecutorThreadPriority = Thread.NORM_PRIORITY;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = 1, max = 31, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int wallExecutorThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    public int wallExecutorThreadPriority = Thread.NORM_PRIORITY;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = 0, max = 8, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    @Config.Access(setter = "setChunkUpdateThreads")
    public int chunkUpdateThreads = 0;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.MAX_PRIORITY)
    public int chunkUpdateThreadPriority = 3;

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
            SeedQueueKeyBindings.resetColumn,
            SeedQueueKeyBindings.resetRow,
            SeedQueueKeyBindings.playNextLock,
            SeedQueueKeyBindings.cancelBenchmark
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

    public boolean hasSimulatedWindowSize() {
        return this.simulatedWindowWidth != 0 && this.simulatedWindowHeight != 0;
    }

    // see Window#calculateScaleFactor
    public int calculateSimulatedScaleFactor(int guiScale, boolean forceUnicodeFont) {
        int scaleFactor = 1;
        while (scaleFactor != guiScale && scaleFactor < this.simulatedWindowWidth && scaleFactor < this.simulatedWindowHeight && this.simulatedWindowWidth / (scaleFactor + 1) >= 320 && this.simulatedWindowHeight / (scaleFactor + 1) >= 240) {
            scaleFactor++;
        }
        if (forceUnicodeFont) {
            scaleFactor += guiScale % 2;
        }
        return scaleFactor;
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        if (JsonObject.class.equals(field.getType())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<JsonObject>(config, this, field, idPrefix)
                    .fromJson(((option, config_, configStorage, optionField, jsonElement) -> option.set(jsonElement.isJsonNull() ? null : jsonElement.getAsJsonObject())))
                    .toJson(((option, config_, configStorage, optionField) -> Optional.ofNullable((JsonElement) option.get()).orElse(JsonNull.INSTANCE)))
                    .createWidget((option, config_, configStorage, optionField) -> new ButtonWidget(0, 0, 150, 20, this.getCustomLayoutText(option.get()), button -> {
                        if (option.get() != null) {
                            option.set(null);
                        } else {
                            String file = TinyFileDialogs.tinyfd_openFileDialog("Upload Custom Wall Layout", null, null, null, false);
                            if (file != null) {
                                try (JsonReader reader = new JsonReader(new FileReader(file))) {
                                    JsonObject jsonObject = new JsonParser().parse(reader).getAsJsonObject();
                                    if (!jsonObject.has("id")) {
                                        jsonObject.add("id", new JsonPrimitive(new File(file).getName()));
                                    }
                                    option.set(jsonObject);
                                } catch (Exception e) {
                                    MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.PACK_COPY_FAILURE, new LiteralText("Failed to load!"), new LiteralText(e.getMessage())));
                                }
                            }
                        }
                        button.setMessage(this.getCustomLayoutText(option.get()));
                    }))
                    .build();
        }
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

    private Text getCustomLayoutText(JsonObject customLayout) {
        if (customLayout == null) {
            return new TranslatableText("speedrunapi.config.seedqueue.option.customLayout.upload");
        }
        if (!customLayout.has("id")) {
            customLayout.add("id", new JsonPrimitive("custom"));
        }
        return new LiteralText(customLayout.get("id").getAsString());
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
