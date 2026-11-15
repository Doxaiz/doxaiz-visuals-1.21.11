package com.lowcode.svintusclient.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lowcode.svintusclient.client.gui.ClickGuiScreen;
import com.lowcode.svintusclient.client.gui.Theme;
import com.lowcode.svintusclient.client.hud.HudManager;
import com.lowcode.svintusclient.client.hud.HudWidget;
import com.lowcode.svintusclient.client.module.Module;
import com.lowcode.svintusclient.client.module.ModuleManager;
import com.lowcode.svintusclient.client.module.setting.BindSetting;
import com.lowcode.svintusclient.client.module.setting.BooleanSetting;
import com.lowcode.svintusclient.client.module.setting.ColorSetting;
import com.lowcode.svintusclient.client.module.setting.ModeSetting;
import com.lowcode.svintusclient.client.module.setting.NumberSetting;
import com.lowcode.svintusclient.client.module.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("doxaiz_visuals.json");
    private static final Path LEGACY_FILE = FabricLoader.getInstance().getConfigDir().resolve("svintusclient.json");

    private Config() {
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("theme", Theme.current().name());
        root.addProperty("theme_preset", Theme.preset().name());
        root.addProperty("visual_style", Theme.style().name());
        root.addProperty("language", com.lowcode.svintusclient.client.util.I18n.getLanguage().code);
        root.addProperty("gui_width", ClickGuiScreen.getWindowWidth());
        root.addProperty("gui_height", ClickGuiScreen.getWindowHeight());

        JsonObject modules = new JsonObject();
        for (Module module : ModuleManager.all()) {
            JsonObject m = new JsonObject();
            m.addProperty("enabled", module.isEnabled());
            m.addProperty("key", module.getKey());
            m.add("settings", writeSettings(module.getSettings()));
            modules.add(module.getName(), m);
        }
        root.add("modules", modules);

        JsonObject hud = new JsonObject();
        for (HudWidget widget : HudManager.widgets()) {
            JsonObject w = new JsonObject();
            w.addProperty("x", widget.getFx());
            w.addProperty("y", widget.getFy());
            w.addProperty("scale", widget.getScale());
            w.add("options", writeSettings(widget.options()));
            hud.add(widget.id, w);
        }
        root.add("hud", hud);

        JsonObject cmdBinds = new JsonObject();
        for (java.util.Map.Entry<Integer, String> entry : com.lowcode.svintusclient.client.module.impl.misc.CommandBindsModule.getBinds().entrySet()) {
            cmdBinds.addProperty(String.valueOf(entry.getKey()), entry.getValue());
        }
        root.add("command_binds", cmdBinds);

        com.google.gson.JsonArray friendsArr = new com.google.gson.JsonArray();
        for (String friend : com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.getFriends()) {
            friendsArr.add(friend);
        }
        root.add("friends", friendsArr);

        com.google.gson.JsonArray waypointsArr = new com.google.gson.JsonArray();
        for (com.lowcode.svintusclient.client.module.impl.render.WaypointsModule.Waypoint wp : com.lowcode.svintusclient.client.module.impl.render.WaypointsModule.getWaypoints()) {
            JsonObject wpObj = new JsonObject();
            wpObj.addProperty("name", wp.name());
            wpObj.addProperty("x", wp.pos().getX());
            wpObj.addProperty("y", wp.pos().getY());
            wpObj.addProperty("z", wp.pos().getZ());
            wpObj.addProperty("color", wp.color());
            waypointsArr.add(wpObj);
        }
        root.add("waypoints", waypointsArr);

        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root));
        } catch (Exception e) {
            SvintusclientClient.LOGGER.error("Failed to save config", e);
        }
    }

    public static void load() {
        Path target = Files.exists(FILE) ? FILE : (Files.exists(LEGACY_FILE) ? LEGACY_FILE : null);
        if (target == null) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(target), JsonObject.class);
            if (root == null) return;
            if (root.has("theme")) {
                try {
                    Theme.setInstant(Theme.Accent.valueOf(root.get("theme").getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (root.has("theme_preset")) {
                try {
                    Theme.setPreset(Theme.ThemePreset.valueOf(root.get("theme_preset").getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (root.has("visual_style")) {
                try {
                    Theme.setStyle(Theme.Style.valueOf(root.get("visual_style").getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (root.has("language")) {
                try {
                    com.lowcode.svintusclient.client.util.I18n.setLanguage(
                            com.lowcode.svintusclient.client.util.I18n.Language.fromCode(root.get("language").getAsString()));
                } catch (Exception ignored) {
                }
            }
            if (root.has("gui_width")) {
                try {
                    ClickGuiScreen.setWindowWidth(root.get("gui_width").getAsFloat());
                } catch (Exception ignored) {
                }
            }
            if (root.has("gui_height")) {
                try {
                    ClickGuiScreen.setWindowHeight(root.get("gui_height").getAsFloat());
                } catch (Exception ignored) {
                }
            }
            JsonObject modules = root.getAsJsonObject("modules");
            if (modules != null) {
                for (Module module : ModuleManager.all()) {
                    JsonObject m = modules.getAsJsonObject(module.getName());
                    if (m == null) continue;
                    if (m.has("enabled")) module.restoreEnabled(m.get("enabled").getAsBoolean());
                    if (m.has("key")) module.setKey(m.get("key").getAsInt());
                    JsonObject settings = m.getAsJsonObject("settings");
                    if (settings != null) readSettings(module.getSettings(), settings);
                }
            }
            JsonObject hud = root.getAsJsonObject("hud");
            if (hud != null) {
                for (HudWidget widget : HudManager.widgets()) {
                    JsonObject w = hud.getAsJsonObject(widget.id);
                    if (w == null) continue;
                    float fx = widget.getFx();
                    float fy = widget.getFy();
                    if (w.has("x")) fx = w.get("x").getAsFloat();
                    if (w.has("y")) fy = w.get("y").getAsFloat();
                    widget.setPosition(fx, fy);
                    if (w.has("scale")) widget.setScale(w.get("scale").getAsFloat());
                    JsonObject options = w.getAsJsonObject("options");
                    if (options != null) readSettings(widget.options(), options);
                }
            }

            JsonObject cmdBinds = root.getAsJsonObject("command_binds");
            if (cmdBinds != null) {
                com.lowcode.svintusclient.client.module.impl.misc.CommandBindsModule.clearBinds();
                for (java.util.Map.Entry<String, JsonElement> e : cmdBinds.entrySet()) {
                    try {
                        int key = Integer.parseInt(e.getKey());
                        String cmd = e.getValue().getAsString();
                        com.lowcode.svintusclient.client.module.impl.misc.CommandBindsModule.setBind(key, cmd);
                    } catch (Exception ignored) {}
                }
            }

            com.google.gson.JsonArray friendsArr = root.getAsJsonArray("friends");
            if (friendsArr != null) {
                com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.getFriends().clear();
                for (JsonElement el : friendsArr) {
                    if (el.isJsonPrimitive()) {
                        com.lowcode.svintusclient.client.module.impl.render.FriendHighlightModule.addFriend(el.getAsString());
                    }
                }
            }

            com.google.gson.JsonArray waypointsArr = root.getAsJsonArray("waypoints");
            if (waypointsArr != null) {
                com.lowcode.svintusclient.client.module.impl.render.WaypointsModule.clearWaypoints();
                for (JsonElement el : waypointsArr) {
                    if (el.isJsonObject()) {
                        JsonObject wpObj = el.getAsJsonObject();
                        String name = wpObj.has("name") ? wpObj.get("name").getAsString() : "Метка";
                        int x = wpObj.has("x") ? wpObj.get("x").getAsInt() : 0;
                        int y = wpObj.has("y") ? wpObj.get("y").getAsInt() : 64;
                        int z = wpObj.has("z") ? wpObj.get("z").getAsInt() : 0;
                        int col = wpObj.has("color") ? wpObj.get("color").getAsInt() : 0xFF00F0FF;
                        com.lowcode.svintusclient.client.module.impl.render.WaypointsModule.getWaypoints().add(
                                new com.lowcode.svintusclient.client.module.impl.render.WaypointsModule.Waypoint(name, new net.minecraft.util.math.BlockPos(x, y, z), col)
                        );
                    }
                }
            }
        } catch (Exception e) {
            SvintusclientClient.LOGGER.error("Failed to load config", e);
        }
    }

    private static JsonObject writeSettings(List<? extends Setting<?>> settings) {
        JsonObject o = new JsonObject();
        for (Setting<?> setting : settings) {
            switch (setting) {
                case BooleanSetting b -> o.addProperty(b.getName(), b.get());
                case NumberSetting n -> o.addProperty(n.getName(), n.get());
                case ModeSetting m -> o.addProperty(m.getName(), m.get());
                case ColorSetting c -> o.addProperty(c.getName(), c.get());
                case BindSetting b -> o.addProperty(b.getName(), b.get());
                default -> {
                }
            }
        }
        return o;
    }

    private static void readSettings(List<? extends Setting<?>> settings, JsonObject o) {
        for (Setting<?> setting : settings) {
            JsonElement el = o.get(setting.getName());
            if (el == null) continue;
            try {
                switch (setting) {
                    case BooleanSetting b -> b.set(el.getAsBoolean());
                    case NumberSetting n -> n.set(el.getAsFloat());
                    case ModeSetting m -> m.set(el.getAsString());
                    case ColorSetting c -> c.set(el.getAsInt());
                    case BindSetting b -> b.set(el.getAsInt());
                    default -> {
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}
