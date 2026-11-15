package com.lowcode.svintusclient.client.module.impl.render;

import com.lowcode.svintusclient.client.module.Category;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class StreamerModeModule extends Module {

    public final BooleanSetting hideName = add(new BooleanSetting("Скрывать никнейм игрока", true));
    public final ModeSetting fakeName = add(new ModeSetting("Псевдоним / Никнейм",
            "Streamer", "Doxaiz_User", "Doxaiz Player", "Protected", "Anonymous", "Player"));
    public final BooleanSetting hideServerIp = add(new BooleanSetting("Скрывать адрес сервера", true));
    public final BooleanSetting hideCoords = add(new BooleanSetting("Скрывать координаты на экране", false));
    public final BooleanSetting protectOthers = add(new BooleanSetting("Скрывать других игроков", false));
    public final BooleanSetting hideScoreboard = add(new BooleanSetting("Скрывать в скорборде", true));
    public final BooleanSetting hideChat = add(new BooleanSetting("Скрывать в чате", true));
    public final BooleanSetting hideTab = add(new BooleanSetting("Скрывать в Tab-листе", true));

    public StreamerModeModule() {
        super("Streamer Mode & Name Protect", "Режим стримера и защита ника: скрытие реального ника, IP сервера и координат", Category.SETTINGS);
    }

    public boolean shouldProtectTab() {
        return isEnabled() && hideTab.get();
    }

    public boolean shouldProtectChat() {
        return isEnabled() && hideChat.get();
    }

    public boolean shouldProtectScoreboard() {
        return isEnabled() && hideScoreboard.get();
    }

    public boolean shouldProtectOthers() {
        return isEnabled() && protectOthers.get();
    }

    public String filterServerIp(String serverIp) {
        if (!isEnabled() || !hideServerIp.get() || serverIp == null) return serverIp;
        return "mc.hidden.net";
    }

    public String replaceName(String input) {
        if (!isEnabled() || !hideName.get() || input == null) return input;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getSession() == null) return input;
        String realName = mc.getSession().getUsername();
        if (realName == null || realName.isEmpty()) return input;

        return input.replace(realName, fakeName.get());
    }

    public String filterText(String text, String realName) {
        if (!isEnabled()) return text;
        if (text == null) return null;
        if (hideName.get() && realName != null && !realName.isEmpty()) {
            text = text.replace(realName, fakeName.get());
        }
        if (hideServerIp.get()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
                text = text.replace(mc.getCurrentServerEntry().address, "mc.hidden.net");
            }
        }
        return text;
    }

    public static String filter(String input) {
        StreamerModeModule mod = ModuleManager.get(StreamerModeModule.class);
        return mod != null ? mod.replaceName(input) : input;
    }
}
