package me.contaria.seedqueue.compat;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.standardsettings.StandardSettingsCache;
import me.contaria.standardsettings.options.PlayerModelPartStandardSetting;
import me.voidxwalker.worldpreview.mixin.access.PlayerEntityAccessor;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SeedQueueSettingsCache extends StandardSettingsCache {

    private static final Set<String> PREVIEW_SETTINGS = new HashSet<>(Arrays.asList(
            "biomeBlendRadius",
            "graphicsMode",
            "renderDistance",
            "ao",
            "guiScale",
            "attackIndicator",
            "gamma",
            "renderClouds",
            "particles",
            "mipmapLevels",
            "entityShadows",
            "entityDistanceScaling",
            "chatVisibility",
            "chatColors",
            "chatOpacity",
            "textBackgroundOpacity",
            "chatScale",
            "chatLineSpacing",
            "chatWidth",
            "chatHeightFocused",
            "chatHeightUnfocused",
            "backgroundForChatOnly",
            "hitboxes",
            "chunkborders",
            "f1"
    ));

    private final Set<Entry<?>> previewSettings;

    public SeedQueueSettingsCache() {
        super(null);

        this.previewSettings = new HashSet<>();
        for (Entry<?> entry : this.cache) {
            if (PREVIEW_SETTINGS.contains(entry.setting.getID())) {
                this.previewSettings.add(entry);
            }
        }
    }

    public void loadPreview() {
        for (Entry<?> cacheEntry : this.previewSettings) {
            cacheEntry.load();
        }
    }

    public void loadPlayerModelParts(ClientPlayerEntity player) {
        int playerModelPartsBitMask = 0;
        for (Entry<?> entry : this.cache) {
            if (entry.setting instanceof PlayerModelPartStandardSetting && Boolean.TRUE.equals(entry.value)) {
                playerModelPartsBitMask |= ((PlayerModelPartStandardSetting) entry.setting).playerModelPart.getBitFlag();
            }
        }
        player.getDataTracker().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), (byte) playerModelPartsBitMask);
    }

    public boolean isCurrentSettings() {
        for (Entry<?> entry : this.cache) {
            if (!Objects.equals(entry.value, entry.setting.getOption())) {
                return false;
            }
        }
        return true;
    }

    public static SeedQueueSettingsCache create() {
        for (SeedQueueSettingsCache settingsCache : SeedQueue.SEED_QUEUE.stream().map(SeedQueueEntry::getWorldPreviewProperties).filter(Objects::nonNull).map(WorldPreviewProperties::getSettingsCache).filter(Objects::nonNull).collect(Collectors.toSet())) {
            if (settingsCache.isCurrentSettings()) {
                System.out.println("test");
                return settingsCache;
            }
        }
        return new SeedQueueSettingsCache();
    }
}
