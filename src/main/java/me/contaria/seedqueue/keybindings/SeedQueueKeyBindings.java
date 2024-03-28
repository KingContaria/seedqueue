package me.contaria.seedqueue.keybindings;

import org.lwjgl.glfw.GLFW;

public class SeedQueueKeyBindings {

    public static SeedQueueMultiKeyBinding play = new SeedQueueMultiKeyBinding("seedqueue.key.play", "seedqueue.key.categories.builtin", GLFW.GLFW_KEY_R);
    public static SeedQueueMultiKeyBinding lock = new SeedQueueMultiKeyBinding("seedqueue.key.lock", "seedqueue.key.categories.builtin", GLFW.GLFW_KEY_L);
    public static SeedQueueMultiKeyBinding reset = new SeedQueueMultiKeyBinding("seedqueue.key.reset", "seedqueue.key.categories.builtin", GLFW.GLFW_KEY_E);
    public static SeedQueueMultiKeyBinding resetAll = new SeedQueueMultiKeyBinding("seedqueue.key.resetAll", "seedqueue.key.categories.builtin", GLFW.GLFW_KEY_T);
    public static SeedQueueMultiKeyBinding focusReset = new SeedQueueMultiKeyBinding("seedqueue.key.focusReset", "seedqueue.key.categories.builtin", GLFW.GLFW_KEY_F);
}
