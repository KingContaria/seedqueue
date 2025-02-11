package me.contaria.seedqueue.gui.config;

import me.contaria.seedqueue.SeedQueueConfig;
import me.contaria.speedrunapi.util.TextUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringRenderable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SeedQueueWindowSizeWidget extends AbstractButtonWidget implements ParentElement {
    private static final StringRenderable X = StringRenderable.plain("X");

    private final SeedQueueConfig.WindowSize windowSize;
    private final TextFieldWidget widthWidget;
    private final TextFieldWidget heightWidget;

    @Nullable
    private Element focused;
    private boolean isDragging;

    public SeedQueueWindowSizeWidget(SeedQueueConfig.WindowSize windowSize) {
        super(0, 0, 150, 20, TextUtil.empty());
        this.windowSize = windowSize;
        this.widthWidget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 65, 20, TextUtil.empty());
        this.widthWidget.setText(String.valueOf(this.windowSize.width()));
        this.widthWidget.setChangedListener(text -> {
            if (text.isEmpty()) {
                // set width to 0 without updating the text
                this.windowSize.setWidth(0);
                return;
            }
            this.windowSize.setWidth(Integer.parseUnsignedInt(text));
            String newText = String.valueOf(this.windowSize.width());
            if (!text.equals(newText)) {
                this.widthWidget.setText(newText);
            }
        });
        this.widthWidget.setTextPredicate(text -> {
            try {
                return text.isEmpty() || Integer.parseUnsignedInt(text) >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
        this.heightWidget = new TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, 65, 20, TextUtil.empty());
        this.heightWidget.setText(String.valueOf(this.windowSize.height()));
        this.heightWidget.setChangedListener(text -> {
            if (text.isEmpty()) {
                this.windowSize.setHeight(0);
                return;
            }
            this.windowSize.setHeight(Integer.parseUnsignedInt(text));
            String newText = String.valueOf(this.windowSize.height());
            if (!text.equals(newText)) {
                this.heightWidget.setText(newText);
            }
        });
        this.heightWidget.setTextPredicate(text -> {
            try {
                return text.isEmpty() || Integer.parseUnsignedInt(text) >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    @Override
    public Optional<Element> hoveredElement(double mouseX, double mouseY) {
        return ParentElement.super.hoveredElement(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ParentElement.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ParentElement.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return ParentElement.super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return ParentElement.super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return ParentElement.super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return ParentElement.super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        return ParentElement.super.charTyped(chr, keyCode);
    }

    @Override
    public void setInitialFocus(@Nullable Element element) {
        ParentElement.super.setInitialFocus(element);
    }

    @Override
    public void focusOn(@Nullable Element element) {
        ParentElement.super.focusOn(element);
    }

    @Override
    public boolean changeFocus(boolean lookForwards) {
        return ParentElement.super.changeFocus(lookForwards);
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.widthWidget.x = this.x;
        this.widthWidget.y = this.y;
        this.widthWidget.render(matrices, mouseX, mouseY, delta);
        this.drawCenteredText(matrices, MinecraftClient.getInstance().textRenderer, X, this.x + this.width / 2, this.y + (this.height - MinecraftClient.getInstance().textRenderer.fontHeight) / 2, 0xFFFFFF);
        this.heightWidget.x = this.x + 85;
        this.heightWidget.y = this.y;
        this.heightWidget.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public List<? extends Element> children() {
        List<Element> children = new ArrayList<>();
        children.add(this.widthWidget);
        children.add(this.heightWidget);
        return children;
    }

    @Override
    public final boolean isDragging() {
        return this.isDragging;
    }

    @Override
    public final void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    @Override
    @Nullable
    public Element getFocused() {
        return this.focused;
    }

    @Override
    public void setFocused(@Nullable Element focused) {
        this.focused = focused;
    }
}
