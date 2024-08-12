package me.contaria.seedqueue.keybindings;

import org.lwjgl.glfw.GLFW;

public class SeedQueueKeyBindings {
    public static final SeedQueueMultiKeyBinding play = new SeedQueueMultiKeyBinding("seedqueue.key.play", GLFW.GLFW_KEY_R);
    public static final SeedQueueMultiKeyBinding lock = new SeedQueueMultiKeyBinding("seedqueue.key.lock", GLFW.GLFW_KEY_L);
    public static final SeedQueueMultiKeyBinding reset = new SeedQueueMultiKeyBinding("seedqueue.key.reset", GLFW.GLFW_KEY_E);
    public static final SeedQueueMultiKeyBinding resetAll = new SeedQueueMultiKeyBinding("seedqueue.key.resetAll", GLFW.GLFW_KEY_T);
    public static final SeedQueueMultiKeyBinding focusReset = new SeedQueueMultiKeyBinding("seedqueue.key.focusReset", GLFW.GLFW_KEY_F);
    public static final SeedQueueMultiKeyBinding resetColumn = new SeedQueueMultiKeyBinding("seedqueue.key.resetColumn");
    public static final SeedQueueMultiKeyBinding resetRow = new SeedQueueMultiKeyBinding("seedqueue.key.resetRow");
    public static final SeedQueueMultiKeyBinding playNextLock = new SeedQueueMultiKeyBinding("seedqueue.key.playNextLock");
    public static final SeedQueueMultiKeyBinding scheduleJoin = new SeedQueueMultiKeyBinding("seedqueue.key.schedule_join");
    public static final SeedQueueMultiKeyBinding startBenchmark = new SeedQueueMultiKeyBinding("seedqueue.key.startBenchmark");
    public static final SeedQueueMultiKeyBinding cancelBenchmark = new SeedQueueMultiKeyBinding("seedqueue.key.cancelBenchmark");
}
