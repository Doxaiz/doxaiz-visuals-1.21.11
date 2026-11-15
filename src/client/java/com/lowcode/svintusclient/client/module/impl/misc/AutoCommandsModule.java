package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.TickListener;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public final class AutoCommandsModule extends Module implements TickListener {

    public final BooleanSetting fixAll = add(new BooleanSetting("/fix all", true));
    public final BooleanSetting heal = add(new BooleanSetting("/heal", true));
    public final BooleanSetting feed = add(new BooleanSetting("/feed", false));
    public final NumberSetting intervalSeconds = add(new NumberSetting("Интервал (сек)", 180.0f, 30.0f, 600.0f, 10.0f));

    private long lastCycleTime = 0L;
    private final List<String> queue = new ArrayList<>();
    private long lastCommandSentTime = 0L;

    public AutoCommandsModule() {
        super("Auto Commands", "Автоматически отправляет команды (/fix all, /heal, /feed) по кулдауну", Category.HOLYWORLD);
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.player.networkHandler == null || mc.world == null) {
            queue.clear();
            return;
        }

        long now = System.currentTimeMillis();
        long intervalMs = (long) (intervalSeconds.get().floatValue() * 1000L);

        if (now - lastCycleTime >= intervalMs) {
            queue.clear();
            if (fixAll.get()) queue.add("fix all");
            if (heal.get()) queue.add("heal");
            if (feed.get()) queue.add("feed");
            lastCycleTime = now;
        }

        if (!queue.isEmpty() && now - lastCommandSentTime >= 3500L) {
            String cmd = queue.remove(0);
            mc.player.networkHandler.sendChatCommand(cmd);
            lastCommandSentTime = now;
        }
    }

    @Override
    protected void onEnable() {
        lastCycleTime = System.currentTimeMillis();
        queue.clear();
    }
}
