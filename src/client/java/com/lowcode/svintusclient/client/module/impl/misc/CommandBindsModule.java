package com.lowcode.svintusclient.client.module.impl.misc;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

public final class CommandBindsModule extends Module {

    public final BooleanSetting showOverlay = add(new BooleanSetting("Оверлей", true));
    public final NumberSetting delay = add(new NumberSetting("Задержка (тики)", 5.0f, 1.0f, 40.0f, 1.0f));
    public final BooleanSetting soundFeedback = add(new BooleanSetting("Звук при нажатии", true));
    public final BooleanSetting hideFromChat = add(new BooleanSetting("Скрывать из лога чата", false));

    private static final Map<Integer, String> BINDS = new HashMap<>();
    private long lastCommandTime = 0;

    static {
        BINDS.put(org.lwjgl.glfw.GLFW.GLFW_KEY_H, "/home");
    }

    public CommandBindsModule() {
        super("Бинды команд", "Быстрое выполнение команд сервера по назначенным клавишам", Category.SETTINGS);
    }

    public static void setBind(int key, String cmd) {
        if (cmd == null || cmd.isBlank()) BINDS.remove(key);
        else BINDS.put(key, cmd.trim());
    }

    public static Map<Integer, String> getBinds() {
        return BINDS;
    }

    public static void clearBinds() {
        BINDS.clear();
    }

    public boolean onKey(int key) {
        if (!isEnabled()) return false;
        long now = System.currentTimeMillis();
        long minInterval = (long) (delay.get() * 50.0f);
        if (now - lastCommandTime < minInterval) return false;

        String cmd = BINDS.get(key);
        if (cmd != null && !cmd.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.getNetworkHandler() != null) {
                lastCommandTime = now;
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                mc.getNetworkHandler().sendChatCommand(cmd);
                if (soundFeedback.get()) com.lowcode.svintusclient.client.util.SoundPreview.play("Click", 0.7f, 1.2f);
                if (showOverlay.get()) {
                    mc.player.sendMessage(Text.literal("§d[Bind] §fВыполнена: §e/" + cmd), true);
                }
                if (!hideFromChat.get() && !showOverlay.get()) {
                    mc.player.sendMessage(Text.literal("§d[Bind] §fВыполнена: §e/" + cmd), false);
                }
                return true;
            }
        }
        return false;
    }
}
