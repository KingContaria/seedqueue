package me.contaria.seedqueue.keybindings;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public class SeedQueueMultiKeyBinding {

    private final String translationKey;
    private final String category;
    private final List<InputUtil.Key> keys = new ArrayList<>();

    public SeedQueueMultiKeyBinding(String translationKey, String category, int code) {
        this(translationKey, category, InputUtil.Type.KEYSYM, code);
    }

    public SeedQueueMultiKeyBinding(String translationKey, String category, InputUtil.Type type, int code) {
        this.translationKey = translationKey;
        this.category = category;
        this.keys.add(type.createFromCode(code));
    }

    public boolean matchesKey(int keyCode, int scanCode) {
        return keyCode == InputUtil.UNKNOWN_KEY.getCode() ? this.matchesPrimary(InputUtil.Type.SCANCODE, scanCode) : this.matchesPrimary(InputUtil.Type.KEYSYM, keyCode) && this.areAdditionalKeysDown();
    }

    public boolean matchesMouse(int code) {
        return this.matchesPrimary(InputUtil.Type.MOUSE, code) && this.areAdditionalKeysDown();
    }

    private boolean matchesPrimary(InputUtil.Type type, int code) {
        return this.getPrimaryKey().getCategory() == type && this.getPrimaryKey().getCode() == code;
    }

    private boolean areAdditionalKeysDown() {
        for (InputUtil.Key key : this.getSecondaryKeys()) {
            if (!InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), key.getCode())) {
                return false;
            }
        }
        return true;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getCategory() {
        return this.category;
    }

    public List<InputUtil.Key> getKeys() {
        return new ArrayList<>(this.keys);
    }

    public InputUtil.Key getKey(int index) {
        return this.keys.get(index);
    }

    public InputUtil.Key getPrimaryKey() {
        return this.getKey(0);
    }

    public List<InputUtil.Key> getSecondaryKeys() {
        return this.getKeys().subList(1, this.keys.size());
    }

    public void setKey(int index, InputUtil.Key key) {
        this.keys.set(index, key);
    }

    public void addKey(InputUtil.Key key) {
        this.keys.add(key);
    }

    public boolean removeKey(int index) {
        if (index == 0) {
            this.keys.set(index, InputUtil.UNKNOWN_KEY);
            return false;
        }
        return this.keys.remove(index) != null;
    }

    public void setKeys(List<InputUtil.Key> keys) {
        this.keys.clear();
        this.keys.addAll(keys);
        if (keys.isEmpty()) {
            this.keys.set(0, InputUtil.UNKNOWN_KEY);
        }
    }
}
