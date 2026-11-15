package com.lowcode.svintusclient.client.module.impl.cosmetics;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import org.lwjgl.glfw.GLFW;

public final class AnimeEmotesModule extends Module implements TickListener {

    public final ModeSetting emote = add(new ModeSetting("Эмоция", "Танец",
            "Поклон", "Волна", "Аплодисменты", "Победный клич", "Указатель"));
    public final NumberSetting speed = add(new NumberSetting("Скорость", 1.0f, 0.5f, 2.0f, 0.1f));
    public final BooleanSetting sound = add(new BooleanSetting("Звук эмоции", true));
    public final BooleanSetting thirdPerson = add(new BooleanSetting("Вид от 3 лица", true));

    private long startTime = 0;

    public AnimeEmotesModule() {
        super("Anime Emotes", "Анимации выразительных эмоций и движений персонажа", Category.COSMETICS);
        setKey(GLFW.GLFW_KEY_B);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            float pitchOffset = getPitchOffset();
            String active = getActiveEmote();
            if (getProgress() > 8.0f) {
                startTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    protected void onEnable() {
        startTime = System.currentTimeMillis();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            if (sound.get()) {
                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
            if (thirdPerson.get() && mc.options.getPerspective().isFirstPerson()) {
                mc.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
            }
        }
    }

    public float getProgress() {
        if (!isEnabled()) return 0.0f;
        long elapsed = System.currentTimeMillis() - startTime;
        return (float) (elapsed / 1000.0 * speed.get());
    }

    public String getActiveEmote() {
        return isEnabled() ? emote.get() : "";
    }

    public float getPitchOffset() {
        if (!isEnabled()) return 0.0f;
        float p = getProgress();
        String e = emote.get();
        if ("Поклон".equals(e)) return (float) Math.sin(p * Math.PI) * 25.0f;
        if ("Танец".equals(e)) return (float) Math.sin(p * 4.0) * 8.0f;
        return 0.0f;
    }
}
