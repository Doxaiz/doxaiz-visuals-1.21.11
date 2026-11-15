package com.lowcode.svintusclient.client.module.client;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public class NotificationsModule extends Module {

    public final NumberSetting duration = add(new NumberSetting("Время показа", 2.5f, 1f, 6f, 0.5f));
    public final ModeSetting position = add(new ModeSetting("Позиция", "Справа внизу", "Справа вверху", "Слева вверху", "Слева внизу"));
    public final BooleanSetting soundAlert = add(new BooleanSetting("Звук уведомления", true));
    public final ColorSetting accentColor = add(new ColorSetting("Цвет полосы", 0xFFFF2A6D));

    public NotificationsModule() {
        super("Notifications", "Уведомления о включении модулей", Category.INTERFACE);
        enableByDefault();
    }
}
