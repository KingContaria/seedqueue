package me.contaria.seedqueue.gui.config;

import me.contaria.seedqueue.keybindings.SeedQueueMultiKeyBinding;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;

public class SeedQueueKeybindingsScreen extends Screen {

    private final Screen parent;
    protected final SeedQueueMultiKeyBinding[] keyBindings;
    protected SeedQueueKeybindingsListWidget.KeyEntry focusedBinding;
    private SeedQueueKeybindingsListWidget keyBindingListWidget;

    public SeedQueueKeybindingsScreen(Screen parent, SeedQueueMultiKeyBinding... keyBindings) {
        super(new TranslatableText("seedqueue.menu.keys"));
        this.parent = parent;
        this.keyBindings = keyBindings;
    }

    @Override
    protected void init() {
        this.keyBindingListWidget = new SeedQueueKeybindingsListWidget(this, this.client);
        this.children.add(this.keyBindingListWidget);
        this.addButton(new ButtonWidget(this.width / 2 - 100, this.height - 27, 200, 20, ScreenTexts.DONE, button -> this.onClose()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.focusedBinding != null) {
            this.focusedBinding.mouseClicked(mouseX, mouseY, button);
            this.focusedBinding = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.focusedBinding != null) {
            this.focusedBinding.keyPressed(keyCode, scanCode, modifiers);
            this.focusedBinding = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.keyBindingListWidget.render(matrices, mouseX, mouseY, delta);
        this.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        assert this.client != null;
        this.client.openScreen(this.parent);
    }
}
