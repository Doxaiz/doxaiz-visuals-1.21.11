package com.lowcode.svintusclient.client.module;

import com.lowcode.svintusclient.client.render.icon.Icon;
import com.lowcode.svintusclient.client.render.icon.Icons;

public enum Category {
    BATTLE("Бой", "combat"),
    VISUALS("Визуалы", "render"),
    COSMETICS("Косметика", "player"),
    INTERFACE("Интерфейс", "client"),
    FRIENDS("Друзья", "heart"),
    UTILITIES("Утилиты", "movement"),
    NAVIGATION("Метки", "compass"),
    SOUNDS("Звуки", "signal"),
    HOLYWORLD("HolyWorld", "activity"),
    SETTINGS("Настройки", "settings");

    public final String title;
    private final String iconName;

    Category(String title, String iconName) {
        this.title = title;
        this.iconName = iconName;
    }

    public Icon icon() {
        return Icons.get(iconName);
    }

    public String getDisplayName() {
        return com.lowcode.svintusclient.client.util.I18n.get(title);
    }
}
