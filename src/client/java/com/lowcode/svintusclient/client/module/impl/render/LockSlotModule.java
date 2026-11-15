package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;

public final class LockSlotModule extends Module {

    public final BooleanSetting slot1 = add(new BooleanSetting("Слот 1", true));
    public final BooleanSetting slot2 = add(new BooleanSetting("Слот 2", false));
    public final BooleanSetting slot3 = add(new BooleanSetting("Слот 3", false));
    public final BooleanSetting slot4 = add(new BooleanSetting("Слот 4", false));
    public final BooleanSetting slot5 = add(new BooleanSetting("Слот 5", false));
    public final BooleanSetting slot6 = add(new BooleanSetting("Слот 6", false));
    public final BooleanSetting slot7 = add(new BooleanSetting("Слот 7", false));
    public final BooleanSetting slot8 = add(new BooleanSetting("Слот 8", false));
    public final BooleanSetting slot9 = add(new BooleanSetting("Слот 9", true));
    public final BooleanSetting lockIcon = add(new BooleanSetting("Отображать замок на слоте", true));
    public final BooleanSetting soundAlert = add(new BooleanSetting("Звук при попытке выброса", true));

    public LockSlotModule() {
        super("item.save", "Блокирует случайный выброс ценных предметов (клавиша Q) из выбранных слотов хотбара", Category.UTILITIES);
    }

    public boolean isSlotLocked(int slotIndex) {
        if (!isEnabled()) return false;
        return switch (slotIndex) {
            case 0 -> slot1.get();
            case 1 -> slot2.get();
            case 2 -> slot3.get();
            case 3 -> slot4.get();
            case 4 -> slot5.get();
            case 5 -> slot6.get();
            case 6 -> slot7.get();
            case 7 -> slot8.get();
            case 8 -> slot9.get();
            default -> false;
        };
    }
}
