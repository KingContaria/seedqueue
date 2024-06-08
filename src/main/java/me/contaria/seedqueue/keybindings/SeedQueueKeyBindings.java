package me.contaria.seedqueue.keybindings;

import org.lwjgl.glfw.GLFW;

public class SeedQueueKeyBindings {
    public static SeedQueueMultiKeyBinding play = new SeedQueueMultiKeyBinding("seedqueue.key.play", GLFW.GLFW_KEY_R);
    public static SeedQueueMultiKeyBinding lock = new SeedQueueMultiKeyBinding("seedqueue.key.lock", GLFW.GLFW_KEY_L);
    public static SeedQueueMultiKeyBinding reset = new SeedQueueMultiKeyBinding("seedqueue.key.reset", GLFW.GLFW_KEY_E);
    public static SeedQueueMultiKeyBinding resetAll = new SeedQueueMultiKeyBinding("seedqueue.key.resetAll", GLFW.GLFW_KEY_T);
    public static SeedQueueMultiKeyBinding focusReset = new SeedQueueMultiKeyBinding("seedqueue.key.focusReset", GLFW.GLFW_KEY_F);
    public static SeedQueueMultiKeyBinding resetColumn = new SeedQueueMultiKeyBinding("seedqueue.key.resetColumn");
    public static SeedQueueMultiKeyBinding resetRow = new SeedQueueMultiKeyBinding("seedqueue.key.resetRow");
    public static SeedQueueMultiKeyBinding playNextLock = new SeedQueueMultiKeyBinding("seedqueue.key.playNextLock");
    public static SeedQueueMultiKeyBinding startBenchmark = new SeedQueueMultiKeyBinding("seedqueue.key.startBenchmark");
    public static SeedQueueMultiKeyBinding cancelBenchmark = new SeedQueueMultiKeyBinding("seedqueue.key.cancelBenchmark");
}
