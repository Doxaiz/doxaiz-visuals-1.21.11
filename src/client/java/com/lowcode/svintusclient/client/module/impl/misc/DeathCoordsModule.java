package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public final class DeathCoordsModule extends Module implements TickListener {

    public final BooleanSetting chatAlert = add(new BooleanSetting("Сообщение в чат", true));
    public final BooleanSetting copyToClipboard = add(new BooleanSetting("Копировать кликом", true));
    public final BooleanSetting autoWaypoint = add(new BooleanSetting("Ставить точку на GPS", true));
    public final BooleanSetting deathSound = add(new BooleanSetting("Звук при смерти", true));
    public final BooleanSetting sendToTeam = add(new BooleanSetting("Отправка в чат команды", false));

    private boolean wasDead = false;
    private BlockPos lastDeathPos = null;

    public DeathCoordsModule() {
        super("Death Coords", "Сохраняет и выводит координаты смерти в чат с возможностью копирования", Category.UTILITIES);
        enableByDefault();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            wasDead = false;
            return;
        }

        boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0.0f;
        if (dead && !wasDead) {
            recordDeath(mc.player.getBlockPos());
        }
        wasDead = dead;
    }

    public void recordDeath(BlockPos pos) {
        if (pos == null) return;
        this.lastDeathPos = pos;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !isEnabled() || !chatAlert.get()) return;

        String coordsStr = String.format(Locale.ROOT, "%d %d %d", pos.getX(), pos.getY(), pos.getZ());
        MutableText text = Text.literal("§c[Смерть] §fКоординаты: §e" + coordsStr + " §7(нажмите для копирования)");
        if (copyToClipboard.get()) {
            text.styled(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(coordsStr)));
        }
        mc.player.sendMessage(text, false);
        if (deathSound.get()) {
            com.lowcode.svintusclient.client.util.SoundPreview.play("Headshot Rust", 0.9f, 0.8f);
        }
        if (autoWaypoint.get()) {
            com.lowcode.svintusclient.client.module.impl.render.WaypointsModule.setDeathPos(pos);
        }
        if (sendToTeam.get() && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendChatMessage("Моя смерть: " + coordsStr);
        }
    }

    public BlockPos getLastDeathPos() {
        return lastDeathPos;
    }
}
