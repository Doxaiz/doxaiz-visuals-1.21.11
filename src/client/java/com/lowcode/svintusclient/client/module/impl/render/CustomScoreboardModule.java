package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;

public final class CustomScoreboardModule extends Module {

    public final ModeSetting style = add(new ModeSetting("Стиль скорборда", "Стеклянный (Glass)", "Ванильный (Vanilla)", "Минималистичный (Minimal)", "Без фона (No Background)", "Неон (Neon)"));
    public final BooleanSetting hideScores = add(new BooleanSetting("Скрыть красные числа", true));
    public final BooleanSetting hide = add(new BooleanSetting("Полностью скрыть скорборд", false));
    public final NumberSetting customX = add(new NumberSetting("Смещение по X", 0.0f, -200.0f, 200.0f, 5.0f));
    public final NumberSetting customY = add(new NumberSetting("Смещение по Y", 0.0f, -150.0f, 150.0f, 5.0f));
    public final NumberSetting scale = add(new NumberSetting("Масштаб", 1.0f, 0.6f, 1.4f, 0.05f));
    public final ColorSetting accentColor = add(new ColorSetting("Цвет акцента", 0xFF00CEC9));

    // Scoreboard HP Integration
    public final BooleanSetting showHp = add(new BooleanSetting("HP игроков в скорборде", true));
    public final ModeSetting hpFormat = add(new ModeSetting("Формат здоровья",
            "Число HP (20 HP)", "Сердечки (10❤)", "Проценты (100%)", "Цветная шкала"));
    public final ModeSetting hpColorMode = add(new ModeSetting("Цвет HP",
            "Градация здоровья (Зел/Желт/Красн)", "Акцент темы", "Радуга"));

    public CustomScoreboardModule() {
        super("Scoreboard", "Кастомизация боковой панели: стили оформления, скрытие очков, масштаб, позиция и HP игроков", Category.INTERFACE);
    }

    public String formatHealth(PlayerEntity player) {
        if (!isEnabled() || !showHp.get() || player == null) return "";

        float hp = player.getHealth() + player.getAbsorptionAmount();
        float maxHp = player.getMaxHealth();
        float ratio = Math.max(0f, Math.min(1f, hp / Math.max(1f, maxHp)));

        String color = "§a";
        if ("Градация здоровья (Зел/Желт/Красн)".equals(hpColorMode.get())) {
            if (ratio < 0.35f) color = "§c";
            else if (ratio < 0.7f) color = "§e";
        } else if ("Акцент темы".equals(hpColorMode.get())) {
            color = "§b";
        }

        String m = hpFormat.get();
        if ("Сердечки (10❤)".equals(m)) {
            int hearts = (int) Math.ceil(hp / 2f);
            return color + " " + hearts + "❤";
        } else if ("Проценты (100%)".equals(m)) {
            int pct = (int) (ratio * 100);
            return color + " " + pct + "%";
        } else if ("Цветная шкала".equals(m)) {
            return color + " [" + String.format("%.0f", hp) + "/" + String.format("%.0f", maxHp) + "]";
        } else {
            return color + " " + String.format("%.0f", hp) + " HP";
        }
    }
}
