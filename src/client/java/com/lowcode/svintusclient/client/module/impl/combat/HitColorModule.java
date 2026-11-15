package com.lowcode.svintusclient.client.module.impl.combat;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class HitColorModule extends Module {

    public final ColorSetting color = add(new ColorSetting("Цвет вспышки", 0xFFFF2255));
    public final NumberSetting intensity = add(new NumberSetting("Интенсивность (%)", 80.0f, 10.0f, 100.0f, 5.0f));
    public final ModeSetting targetFilter = add(new ModeSetting("Цели для смены цвета",
            "Все сущности", "Только игроки", "Враги (не друзья)"));

    private static final Map<LivingEntityRenderState, Integer> RENDER_TINTS = Collections.synchronizedMap(new WeakHashMap<>());

    public HitColorModule() {
        super("HitColor", "Смена цвета и вспышка противника при получении урона", Category.BATTLE);
    }

    public boolean shouldTint(LivingEntity entity) {
        if (!isEnabled() || entity == null) return false;
        if (entity.hurtTime <= 0) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity == mc.player) return false;

        String tf = targetFilter.get();
        if ("Только игроки".equals(tf)) {
            return entity instanceof PlayerEntity;
        } else if ("Враги (не друзья)".equals(tf)) {
            if (entity instanceof PlayerEntity p) {
                return !com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.isFriend(p.getName().getString());
            }
            return true;
        }
        return true;
    }

    public void captureTint(LivingEntityRenderState state, LivingEntity entity) {
        if (!isEnabled()) {
            RENDER_TINTS.remove(state);
            return;
        }
        if (shouldTint(entity)) {
            RENDER_TINTS.put(state, getEffectiveTint());
        } else {
            RENDER_TINTS.remove(state);
        }
    }

    public Integer tintFor(LivingEntityRenderState state) {
        return isEnabled() ? RENDER_TINTS.get(state) : null;
    }

    public int getEffectiveTint() {
        int a = Math.round((intensity.get() / 100.0f) * 255.0f);
        if (a <= 0) a = 180;
        int rgb = color.getEffectiveColor() & 0x00FFFFFF;
        return (a << 24) | rgb;
    }

    public void onEntityHit(LivingEntity entity) {
        // Immediate hit state
    }

    @Override
    protected void onDisable() {
        RENDER_TINTS.clear();
    }
}
