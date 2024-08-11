package me.contaria.seedqueue.compat;

import me.contaria.standardsettings.StandardSettings;

class StandardSettingsCompat {

    static void reset() {
        StandardSettings.reset();
    }

    static void createCache() {
        StandardSettings.createCache();
    }

    static void resetPendingActions() {
        StandardSettings.resetPendingActions();
    }

    static void onWorldJoin() {
        StandardSettings.onWorldJoin();
    }

    static void loadCache() {
        StandardSettings.loadCache(StandardSettings.lastWorld);
    }
}
