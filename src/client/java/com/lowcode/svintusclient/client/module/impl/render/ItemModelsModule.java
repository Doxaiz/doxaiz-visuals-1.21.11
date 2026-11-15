package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class ItemModelsModule extends Module {

    public static final String[] TOTEMS = new String[]{
        "Ванильный", "Furry", "Gryaznaya", "Mita_Sleepy", "Mita_Mila", "Mita_Cap", "Mita_Kind",
        "Rimuru", "Shape", "Skull", "SweetieFox", "Anime_Alisa", "Anime_Alya", "Anime_Missa"
    };

    public final ModeSetting totemStyle = add(new ModeSetting("Модель тотема", TOTEMS));
    public final BooleanSetting selfOnly = add(new BooleanSetting("Только у себя", true));
    public final NumberSetting handScale = add(new NumberSetting("Масштаб в руке", 1.0f, 0.6f, 1.6f, 0.05f));
    public final ModeSetting glintColor = add(new ModeSetting("Цвет чар (Glint)",
            "Ванильный", "Фиолетовый", "Хрома / Радуга", "Золотой", "Циан", "Неоново-розовый", "Изумрудный"));
    public final NumberSetting glintSpeed = add(new NumberSetting("Скорость чар", 1.0f, 0.2f, 3.0f, 0.1f));
    public final BooleanSetting glintShine = add(new BooleanSetting("Интенсивный блеск", true));

    public float getHandScale() {
        return isEnabled() ? handScale.get() : 1.0f;
    }

    public boolean hasIntenseShine() {
        return isEnabled() && glintShine.get();
    }

    public ItemModelsModule() {
        super("Item Models", "Кастомные визуальные 3D модели тотемов и цвет свечения чар", Category.COSMETICS);
    }

    public String getSelectedTotem() {
        return totemStyle.get();
    }

    public int getGlintColor() {
        if (!isEnabled()) return -1;
        return switch (glintColor.get()) {
            case "Фиолетовый" -> 0xFF8A2BE2;
            case "Хрома / Радуга" -> {
                float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
                yield java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f);
            }
            case "Золотой" -> 0xFFFFD700;
            case "Циан" -> 0xFF00FFFF;
            case "Неоново-розовый" -> 0xFFFF2A6D;
            case "Изумрудный" -> 0xFF2ED573;
            default -> -1;
        };
    }
}
