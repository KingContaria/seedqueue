package me.contaria.seedqueue.gui.config;

import me.contaria.seedqueue.keybindings.SeedQueueMultiKeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class SeedQueueKeybindingsListWidget extends ElementListWidget<SeedQueueKeybindingsListWidget.Entry> {

    private final SeedQueueKeybindingsScreen parent;

    public SeedQueueKeybindingsListWidget(SeedQueueKeybindingsScreen parent, MinecraftClient client) {
        super(client, parent.width + 45, parent.height, 43, parent.height - 32, 25);
        this.parent = parent;

        Map<String, List<KeyEntry>> categoryToKeyEntryMap = new LinkedHashMap<>();
        for (SeedQueueMultiKeyBinding keyBinding : parent.keyBindings) {
            categoryToKeyEntryMap.computeIfAbsent(keyBinding.getCategory(), category -> new ArrayList<>()).add(new KeyEntry(keyBinding));
        }
        for (Map.Entry<String, List<KeyEntry>> category : categoryToKeyEntryMap.entrySet()) {
            this.addEntry(new CategoryEntry(new TranslatableText(category.getKey())));
            for (KeyEntry keyEntry : category.getValue()) {
                this.addEntry(keyEntry);
            }
        }
    }

    @Override
    public int getRowWidth() {
        return this.parent.width;
    }

    public abstract static class Entry extends ElementListWidget.Entry<Entry> {
    }

    public class CategoryEntry extends Entry {
        private final Text text;
        private final int textWidth;

        public CategoryEntry(Text text) {
            this.text = text;
            this.textWidth = SeedQueueKeybindingsListWidget.this.client.textRenderer.getWidth(this.text);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            SeedQueueKeybindingsListWidget.this.client.textRenderer.draw(matrices, this.text, (SeedQueueKeybindingsListWidget.this.parent.width - this.textWidth) / 2.0f, (float)(y + entryHeight - SeedQueueKeybindingsListWidget.this.client.textRenderer.fontHeight - 1), 0xFFFFFF);
        }

        @Override
        public boolean changeFocus(boolean lookForwards) {
            return false;
        }

        @Override
        public List<? extends Element> children() {
            return Collections.emptyList();
        }
    }

    public class KeyEntry extends Entry {
        private final SeedQueueMultiKeyBinding binding;
        private final Text name;
        private final List<ButtonWidget> keyBindingButtons = new ArrayList<>();
        private final ButtonWidget addKeyButton;
        private int focusedKey = 0;

        private KeyEntry(SeedQueueMultiKeyBinding keyBinding) {
            this.binding = keyBinding;
            this.name = new TranslatableText(keyBinding.getTranslationKey());
            for (int i = 0; i < this.binding.getKeys().size(); i++) {
                this.keyBindingButtons.add(this.createKeyBindingButton());
            }
            this.addKeyButton = new ButtonWidget(0, 0, 20, 20, new LiteralText("+"), button -> {
                this.keyBindingButtons.add(this.createKeyBindingButton());
                this.binding.addKey(InputUtil.UNKNOWN_KEY);
                SeedQueueKeybindingsListWidget.this.parent.focusedBinding = this;
                this.focusedKey = this.keyBindingButtons.size() - 1;
            });
        }

        private ButtonWidget createKeyBindingButton() {
            return new ButtonWidget(0, 0, 75, 20, LiteralText.EMPTY, button -> {
                SeedQueueKeybindingsListWidget.this.parent.focusedBinding = this;
                this.focusedKey = this.keyBindingButtons.indexOf(button);
            }) {
                @SuppressWarnings("WrongTypeInTranslationArgs")
                @Override
                protected MutableText getNarrationMessage() {
                    InputUtil.Key key = KeyEntry.this.binding.getKey(KeyEntry.this.keyBindingButtons.indexOf(this));
                    if (key.equals(InputUtil.UNKNOWN_KEY)) {
                        return new TranslatableText("narrator.controls.unbound", KeyEntry.this.name);
                    }
                    return new TranslatableText("narrator.controls.bound", KeyEntry.this.name, key.getLocalizedText());
                }
            };
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            x += 10;
            SeedQueueKeybindingsListWidget.this.client.textRenderer.draw(matrices, this.name, x, (float)(y + entryHeight / 2 - SeedQueueKeybindingsListWidget.this.client.textRenderer.fontHeight / 2), 0xFFFFFF);
            x += 120;

            int xOffset = 5;
            for (ButtonWidget keyBindingButton : this.keyBindingButtons) {
                this.renderKeyBindingButton(keyBindingButton, this.binding.getKey(this.keyBindingButtons.indexOf(keyBindingButton)), x, y, matrices, mouseX, mouseY, tickDelta);
                x += keyBindingButton.getWidth() + xOffset;
                xOffset = 0;
            }
            x += 5;

            this.addKeyButton.x = x;
            this.addKeyButton.y = y;
            this.addKeyButton.render(matrices, mouseX, mouseY, tickDelta);
        }

        private void renderKeyBindingButton(ButtonWidget button, InputUtil.Key key, int x, int y, MatrixStack matrices, int mouseX, int mouseY, float tickDelta) {
            button.x = x;
            button.y = y;
            button.setMessage(key.getLocalizedText());
            if (SeedQueueKeybindingsListWidget.this.parent.focusedBinding == this && this.focusedKey == this.keyBindingButtons.indexOf(button)) {
                button.setMessage(new LiteralText("> ").append(button.getMessage().shallowCopy()).append(" <").formatted(Formatting.YELLOW));
            }
            button.render(matrices, mouseX, mouseY, tickDelta);
        }

        @Override
        public List<? extends Element> children() {
            List<Element> children = new ArrayList<>(this.keyBindingButtons);
            children.add(this.addKeyButton);
            return children;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (SeedQueueKeybindingsListWidget.this.parent.focusedBinding == this) {
                InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
                if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                    this.binding.setKey(this.focusedKey, key);
                } else {
                    if (this.binding.removeKey(this.focusedKey)) {
                        this.keyBindingButtons.remove(this.focusedKey);
                    }
                }
                this.focusedKey = 0;
                return true;
            }
            for (Element element : this.children()) {
                if (element.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (SeedQueueKeybindingsListWidget.this.parent.focusedBinding == this) {
                this.binding.setKey(this.focusedKey, InputUtil.Type.MOUSE.createFromCode(button));
                this.focusedKey = 0;
                return true;
            }
            for (Element element : this.children()) {
                if (element.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            for (Element element : this.children()) {
                if (element.mouseReleased(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }
    }
}
