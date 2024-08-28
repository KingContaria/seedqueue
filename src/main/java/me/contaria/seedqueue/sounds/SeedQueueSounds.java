package me.contaria.seedqueue.sounds;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SeedQueueSounds {
    public static final SoundEvent PLAY_INSTANCE = register("play_instance");
    public static final SoundEvent LOCK_INSTANCE = register("lock_instance");
    public static final SoundEvent RESET_INSTANCE = register("reset_instance");
    public static final SoundEvent RESET_ALL = register("reset_all");
    public static final SoundEvent RESET_COLUMN = register("reset_column");
    public static final SoundEvent RESET_ROW = register("reset_row");
    public static final SoundEvent SCHEDULE_JOIN = register("schedule_join");
    public static final SoundEvent SCHEDULE_ALL = register("schedule_all");
    public static final SoundEvent SCHEDULED_JOIN_WARNING = register("scheduled_join_warning");
    public static final SoundEvent START_BENCHMARK = register("start_benchmark");
    public static final SoundEvent FINISH_BENCHMARK = register("finish_benchmark");
    public static final SoundEvent OPEN_WALL = register("open_wall");
    public static final SoundEvent BYPASS_WALL = register("bypass_wall");

    public static void init() {
    }

    private static SoundEvent register(String id) {
        return register(new Identifier("seedqueue", id));
    }

    private static SoundEvent register(Identifier id) {
        return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }

    public static boolean play(SoundEvent sound) {
        SoundManager manager = MinecraftClient.getInstance().getSoundManager();
        SoundInstance soundInstance = PositionedSoundInstance.master(sound, 1.0f);
        soundInstance.getSoundSet(manager);
        if (soundInstance.getSound().equals(SoundManager.MISSING_SOUND)) {
            return false;
        }
        manager.play(soundInstance);
        return true;
    }

    public static boolean play(SoundEvent sound, int delay) {
        SoundManager manager = MinecraftClient.getInstance().getSoundManager();
        SoundInstance soundInstance = PositionedSoundInstance.master(sound, 1.0f);
        soundInstance.getSoundSet(manager);
        if (soundInstance.getSound().equals(SoundManager.MISSING_SOUND)) {
            return false;
        }
        manager.play(soundInstance, delay);
        return true;
    }
}
