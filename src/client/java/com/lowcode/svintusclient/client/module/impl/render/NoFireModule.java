package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;

public final class NoFireModule extends Module {

    public final BooleanSetting hideScreenFire = add(new BooleanSetting("Огонь на экране", true));
    public final BooleanSetting lowFire = add(new BooleanSetting("Низкий огонь (Low Fire)", true));
    public final NumberSetting fireHeight = add(new NumberSetting("Высота пламени", 0.3f, 0.0f, 1.0f, 0.05f));
    public final BooleanSetting hideEntityFire = add(new BooleanSetting("Огонь на игроках", false));
    public final BooleanSetting blueFire = add(new BooleanSetting("Синий огонь душ", false));

    public float getEffectiveHeight() {
        if (!isEnabled()) return 1.0f;
        if (hideScreenFire.get()) return 0.0f;
        return lowFire.get() ? fireHeight.get() : 1.0f;
    }

    public boolean isBlueFire() {
        return isEnabled() && blueFire.get();
    }

    public NoFireModule() {
        super("No Fire", "Скрывает или понижает текстуру огня от первого лица", Category.UTILITIES);
    }
}
