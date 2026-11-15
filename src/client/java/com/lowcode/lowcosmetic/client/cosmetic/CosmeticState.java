package com.lowcode.lowcosmetic.client.cosmetic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

public final class CosmeticState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("lowcosmetic.json");

    private static final Map<CosmeticType, Cosmetic> EQUIPPED = new EnumMap<>(CosmeticType.class);
    private static final Set<Integer> FAVORITES = new LinkedHashSet<>();
    private static boolean visible = true;
    private static Cosmetic preview;
    private static boolean forceVisible;

    private CosmeticState() {
    }

    public static boolean isEquipped(Cosmetic cosmetic) {
        return EQUIPPED.get(cosmetic.type()) == cosmetic;
    }

    public static Cosmetic equipped(CosmeticType type) {
        return EQUIPPED.get(type);
    }

    public static int equippedCount() {
        return EQUIPPED.size();
    }

    public static boolean toggle(Cosmetic cosmetic) {
        boolean equip = !isEquipped(cosmetic);
        if (equip) {
            EQUIPPED.put(cosmetic.type(), cosmetic);
        } else {
            EQUIPPED.remove(cosmetic.type());
        }
        save();
        return equip;
    }

    public static void equip(Cosmetic cosmetic) {
        if (cosmetic != null) {
            EQUIPPED.put(cosmetic.type(), cosmetic);
            save();
        }
    }

    public static void unequip(CosmeticType type) {
        if (EQUIPPED.remove(type) != null) {
            save();
        }
    }

    public static void unequipAll() {
        EQUIPPED.clear();
        save();
    }

    public static boolean isFavorite(Cosmetic cosmetic) {
        return FAVORITES.contains(cosmetic.id());
    }

    public static void toggleFavorite(Cosmetic cosmetic) {
        if (!FAVORITES.remove(cosmetic.id())) {
            FAVORITES.add(cosmetic.id());
        }
        save();
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean value) {
        visible = value;
        save();
    }

    public static void setPreview(Cosmetic cosmetic) {
        preview = cosmetic;
    }

    public static void setForceVisible(boolean value) {
        forceVisible = value;
    }

    public static List<Cosmetic> wornBy(int entityId) {
        if (!isLocalPlayer(entityId) || !(visible || forceVisible)) {
            return List.of();
        }
        Map<CosmeticType, Cosmetic> worn = new EnumMap<>(EQUIPPED);
        if (preview != null) {
            worn.put(preview.type(), preview);
        }
        return new ArrayList<>(worn.values());
    }

    private static boolean isLocalPlayer(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player != null && entityId == client.player.getId();
    }

    public static void load() {
        EQUIPPED.clear();
        FAVORITES.clear();
        if (!Files.exists(FILE)) {
            return;
        }
        try {
            JsonObject json = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8)).getAsJsonObject();
            visible = !json.has("visible") || json.get("visible").getAsBoolean();
            if (json.has("equipped")) {
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("equipped").entrySet()) {
                    Cosmetic cosmetic = CosmeticRegistry.get(entry.getValue().getAsInt());
                    if (cosmetic != null && cosmetic.type().id.equals(entry.getKey())) {
                        EQUIPPED.put(cosmetic.type(), cosmetic);
                    }
                }
            }
            if (json.has("favorites")) {
                for (JsonElement element : json.getAsJsonArray("favorites")) {
                    FAVORITES.add(element.getAsInt());
                }
            }
        } catch (Exception e) {
            LowcosmeticClient.LOGGER.error("Failed to read {}", FILE, e);
        }
    }

    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("visible", visible);
        JsonObject equipped = new JsonObject();
        EQUIPPED.forEach((type, cosmetic) -> equipped.addProperty(type.id, cosmetic.id()));
        json.add("equipped", equipped);
        JsonArray favorites = new JsonArray();
        FAVORITES.forEach(favorites::add);
        json.add("favorites", favorites);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LowcosmeticClient.LOGGER.error("Failed to write {}", FILE, e);
        }
    }
}
