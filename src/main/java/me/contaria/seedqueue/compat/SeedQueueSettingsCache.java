package me.contaria.seedqueue.compat;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.standardsettings.StandardSettingsCache;
import me.contaria.standardsettings.options.PlayerModelPartStandardSetting;
import me.contaria.standardsettings.options.StandardSetting;
import me.voidxwalker.worldpreview.mixin.access.PlayerEntityAccessor;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Saves settings when a preview is first rendered to load them when the corresponding {@link SeedQueueEntry} is entered.
 * <p>
 * This is to achieve parity with multi instance settings changing.
 * If settings wouldn't get cached it would allow people to view preview A, then enter preview B,
 * change settings (for example the piechart) to values best suited for preview A, leave preview B and then enter preview A with optimal settings.
 */
public class SeedQueueSettingsCache extends StandardSettingsCache {
    // a set of all setting id's that affect preview rendering on wall
    // while language and forceUnicodeFont also affect previews, they require reloading of some resources which is not feasible
    // does not include perspective since it is special cased
    // see WorldPreviewProperties#getPerspective
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
            "textBackgroundOpacity",
            "backgroundForChatOnly",
            "hitboxes",
            "chunkborders",
            "f1"
    ));

    // ignored settings dont get saved in seedqueue settings caches
    // fullscreen is ignored since it can also be changed by macros at any point and is quite annoying when it is changed during a session
    private static final Set<String> IGNORED_SETTINGS = new HashSet<>(Collections.singletonList(
            "fullscreen"
    ));

    private final Set<Entry<?>> previewSettings;

    private SeedQueueSettingsCache() {
        super(null);

        this.cache.removeIf(entry -> IGNORED_SETTINGS.contains(entry.setting.getID()));

        this.previewSettings = new HashSet<>();
        for (Entry<?> entry : this.cache) {
            if (PREVIEW_SETTINGS.contains(entry.setting.getID())) {
                this.previewSettings.add(entry);
            }
        }
    }

    /**
     * Loads only settings that affect preview rendering.
     *
     * @see SeedQueueSettingsCache#PREVIEW_SETTINGS
     */
    public void loadPreview() {
        for (Entry<?> entry : this.previewSettings) {
            entry.load();
        }
    }

    /**
     * Loads player model part settings onto the given {@link ClientPlayerEntity}.
     */
    public void loadPlayerModelParts(ClientPlayerEntity player) {
        int playerModelPartsBitMask = 0;
        for (Entry<?> entry : this.cache) {
            if (entry.setting instanceof PlayerModelPartStandardSetting && Boolean.TRUE.equals(entry.value)) {
                playerModelPartsBitMask |= ((PlayerModelPartStandardSetting) entry.setting).playerModelPart.getBitFlag();
            }
        }
        player.getDataTracker().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), (byte) playerModelPartsBitMask);
    }

    /**
     * @param option The options ID according to {@link StandardSetting#getID}.
     * @return The cached value for the given option.
     */
    public Object getValue(String option) {
        for (Entry<?> entry : this.cache) {
            if (option.equals(entry.setting.getID())) {
                return entry.value;
            }
        }
        return null;
    }

    /**
     * @return True if the cached settings match the current settings.
     */
    public boolean isCurrentSettings() {
        for (Entry<?> entry : this.cache) {
            if (!Objects.equals(entry.value, entry.setting.getOption())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Searches all {@link SeedQueueEntry}s and returns the first {@link SeedQueueSettingsCache} matching the current settings or creates a new cache if it doesn't find one.
     *
     * @return A {@link SeedQueueSettingsCache} matching the current settings.
     * @see SeedQueueSettingsCache#isCurrentSettings
     */
    public static SeedQueueSettingsCache create() {
        for (SeedQueueSettingsCache settingsCache : SeedQueue.getEntries().stream().map(SeedQueueEntry::getSettingsCache).filter(Objects::nonNull).collect(Collectors.toSet())) {
            if (settingsCache.isCurrentSettings()) {
                return settingsCache;
            }
        }
        return new SeedQueueSettingsCache();
    }
}
