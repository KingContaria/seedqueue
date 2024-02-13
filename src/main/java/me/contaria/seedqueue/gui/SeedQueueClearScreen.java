package me.contaria.seedqueue.gui;

import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableText;

public class SeedQueueClearScreen extends SaveLevelScreen {

    private final Screen parent;

    public SeedQueueClearScreen(Screen parent) {
        super(new TranslatableText("seedqueue.menu.clearing"));
        this.parent = parent;
    }


    @Override
    public void onClose() {
        assert this.client != null;
        this.client.openScreen(this.parent);
    }
}
