package com.lowcode.lowcosmetic.client.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class UiSettings {
    public static final float[] SCALES = {0.8F, 0.9F, 1.0F, 1.1F, 1.25F};

    public enum Accent {
        VIOLET("Фиолетовый", 0xFF8B5CF6),
        BLUE("Синий", 0xFF3B82F6),
        CYAN("Бирюзовый", 0xFF14B8C6),
        GREEN("Зелёный", 0xFF22C55E),
        GOLD("Золотой", 0xFFEAB308),
        ORANGE("Оранжевый", 0xFFF97316),
        RED("Красный", 0xFFEF4444),
        PINK("Розовый", 0xFFEC4899);

        public final String label;
        public final int color;

        Accent(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    public enum Font {
        GOLOS("Golos", "golos"),
        INTER("Inter", "inter"),
        MONTSERRAT("Montserrat", "montserrat"),
        NUNITO("Nunito", "nunito"),
        MINECRAFT("Minecraft", null);

        public final String label;
        public final String id;

        Font(String label, String id) {
            this.label = label;
            this.id = id;
        }
    }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("lowcosmetic_ui.json");

    public static int scaleIndex = 2;
    public static Accent accent = Accent.VIOLET;
    public static Font font = Font.GOLOS;
    public static boolean sounds = true;
    public static boolean blur = true;
    public static boolean spinCards = true;

    private UiSettings() {
    }

    public static float scale() {
        return SCALES[Math.max(0, Math.min(SCALES.length - 1, scaleIndex))];
    }

    public static void reset() {
        scaleIndex = 2;
        accent = Accent.VIOLET;
        font = Font.GOLOS;
        sounds = true;
        blur = true;
        spinCards = true;
        save();
    }

    public static void load() {
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            if (json.has("scale")) {
                scaleIndex = Math.max(0, Math.min(SCALES.length - 1, json.get("scale").getAsInt()));
            }
            if (json.has("accent")) {
                try {
                    accent = Accent.valueOf(json.get("accent").getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (json.has("font")) {
                try {
                    font = Font.valueOf(json.get("font").getAsString());
                } catch (IllegalArgumentException ignored) {
                }
            }
            sounds = !json.has("sounds") || json.get("sounds").getAsBoolean();
            blur = !json.has("blur") || json.get("blur").getAsBoolean();
            spinCards = !json.has("spinCards") || json.get("spinCards").getAsBoolean();
        } catch (Exception e) {
            LowcosmeticClient.LOGGER.error("Failed to read {}", FILE, e);
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("scale", scaleIndex);
        json.addProperty("accent", accent.name());
        json.addProperty("font", font.name());
        json.addProperty("sounds", sounds);
        json.addProperty("blur", blur);
        json.addProperty("spinCards", spinCards);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LowcosmeticClient.LOGGER.error("Failed to write {}", FILE, e);
        }
    }
}
