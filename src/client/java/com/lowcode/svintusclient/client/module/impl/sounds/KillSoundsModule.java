package com.lowcode.svintusclient.client.module.impl.sounds;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.util.SoundPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

public final class KillSoundsModule extends Module {

    public final ModeSetting sound = add(new ModeSetting("Звук убийства",
            "Final Pok", "Headshot Rust", "Neptune", "Abmiss", "Whii",
            "Critow", "Final Blink", "Молния", "Опыт", "Крит"));
    public final NumberSetting volume = add(new NumberSetting("Громкость", 0.9f, 0.1f, 1.0f, 0.05f));
    public final NumberSetting pitch = add(new NumberSetting("Тон (Pitch)", 1.0f, 0.5f, 1.5f, 0.05f));
    public final BooleanSetting chatAlert = add(new BooleanSetting("Оповещение в чат", false));
    public final BooleanSetting pitchRandomize = add(new BooleanSetting("Случайный тон", false));
    public final BooleanSetting streakSound = add(new BooleanSetting("Повышать тон со стриком", false));

    private int killStreak = 0;
    private long lastKillTime = 0;

    public KillSoundsModule() {
        super("Kill Sounds", "Кастомные звуковые оповещения и звуки победы при убийстве игрока", Category.SOUNDS);
    }

    public void playKillSound(LivingEntity victim) {
        if (!isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastKillTime < 10000L) {
            killStreak++;
        } else {
            killStreak = 1;
        }
        lastKillTime = now;

        float p = pitch.get();
        if (streakSound.get()) {
            p = Math.min(2.0f, p + (killStreak - 1) * 0.1f);
        } else if (pitchRandomize.get()) {
            p += (float) ((Math.random() - 0.5) * 0.15);
        }
        SoundPreview.play(sound.get(), volume.get(), p);

        if (chatAlert.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                String victimName = victim != null ? victim.getName().getString() : "противник";
                String msg = "§6⚔ §fУбит: §c" + victimName + (killStreak > 1 ? " §e(Серия x" + killStreak + "!)" : "");
                mc.player.sendMessage(Text.literal(msg), false);
            }
        }
    }

    public void playKillSound() {
        playKillSound(null);
    }
}
