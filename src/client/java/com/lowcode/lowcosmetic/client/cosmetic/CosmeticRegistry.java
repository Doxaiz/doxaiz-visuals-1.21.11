package com.lowcode.lowcosmetic.client.cosmetic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowcode.lowcosmetic.client.LowcosmeticClient;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import com.lowcode.lowcosmetic.client.geo.GeoModelParser;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class CosmeticRegistry {
    private static final Identifier CATALOG = LowcosmeticClient.id("cosmetics/catalog.json");

    private static List<Cosmetic> all = List.of();
    private static Map<Integer, Cosmetic> byId = Map.of();

    private CosmeticRegistry() {
    }

    public static List<Cosmetic> all() {
        return all;
    }

    public static Cosmetic get(int id) {
        return byId.get(id);
    }

    public static void load(ResourceManager resources) {
        List<Cosmetic> loaded = new ArrayList<>();
        try (Reader reader = resources.openAsReader(CATALOG)) {
            JsonObject catalog = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement element : catalog.getAsJsonArray("cosmetics")) {
                JsonObject entry = element.getAsJsonObject();
                try {
                    Cosmetic cosmetic = loadOne(resources, entry);
                    if (cosmetic != null) {
                        loaded.add(cosmetic);
                    }
                } catch (Exception e) {
                    LowcosmeticClient.LOGGER.error("Failed to load cosmetic {}", entry, e);
                }
            }
        } catch (Exception e) {
            LowcosmeticClient.LOGGER.error("Failed to read cosmetic catalog", e);
        }

        loaded.sort(Comparator.comparing(Cosmetic::type)
            .thenComparing(Cosmetic::rarity, Comparator.reverseOrder())
            .thenComparing(Cosmetic::name));
        Map<Integer, Cosmetic> index = new HashMap<>();
        for (Cosmetic cosmetic : loaded) {
            index.put(cosmetic.id(), cosmetic);
        }
        all = Collections.unmodifiableList(loaded);
        byId = index;
        LowcosmeticClient.LOGGER.info("Loaded {} cosmetics", all.size());
    }

    private static Cosmetic loadOne(ResourceManager resources, JsonObject entry) throws Exception {
        int id = entry.get("id").getAsInt();
        CosmeticType type = CosmeticType.byId(entry.get("type").getAsString());
        if (type == null) {
            LowcosmeticClient.LOGGER.warn("Cosmetic {} has unknown type {}", id, entry.get("type"));
            return null;
        }

        JsonObject json;
        try (InputStream in = resources.open(LowcosmeticClient.id("cosmetics/models/cosmetic_" + id + ".json"))) {
            json = JsonParser.parseString(stripBom(new String(in.readAllBytes(), StandardCharsets.UTF_8))).getAsJsonObject();
        }

        NativeImage[] image = new NativeImage[1];
        Identifier texture = texture(resources, id, json.has("texture") ? json.get("texture").getAsString() : "", image);
        if (texture == null) {
            LowcosmeticClient.LOGGER.info("Cosmetic {} skipped: texture is missing", id);
            return null;
        }

        if (!json.has("model")) {
            LowcosmeticClient.LOGGER.warn("Cosmetic {} has no model", id);
            return null;
        }
        CosmeticModel model = CosmeticModel.parse(json, image[0] != null ? alphaTest(image[0]) : GeoModelParser.AlphaTest.ALWAYS);

        String name = entry.has("name") ? entry.get("name").getAsString() : json.get("name").getAsString();
        Rarity rarity = Rarity.byId(entry.has("rarity") ? entry.get("rarity").getAsString() : "common");
        return new Cosmetic(id, name, type, rarity, texture, model);
    }

    private static Identifier texture(ResourceManager resources, int id, String value, NativeImage[] imageOut) throws Exception {
        if (value.isEmpty()) {
            return null;
        }

        Identifier asId = value.endsWith(".png") ? Identifier.tryParse(value) : null;
        if (asId != null) {
            Optional<Resource> resource = resources.getResource(asId);
            return resource.isPresent() ? asId : null;
        }

        int comma = value.indexOf(',');
        String base64 = value.startsWith("data:") && comma >= 0 ? value.substring(comma + 1) : value;
        NativeImage image = NativeImage.read(Base64.getMimeDecoder().decode(base64));
        imageOut[0] = image;
        Identifier textureId = LowcosmeticClient.id("cosmetic/" + id);
        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(() -> "cosmetic_" + id, image));
        return textureId;
    }

    private static GeoModelParser.AlphaTest alphaTest(NativeImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        return (u0, v0, u1, v1) -> {
            int x0 = Math.max(0, (int) Math.floor(u0 * w));
            int y0 = Math.max(0, (int) Math.floor(v0 * h));
            int x1 = Math.min(w, Math.max(x0 + 1, (int) Math.ceil(u1 * w)));
            int y1 = Math.min(h, Math.max(y0 + 1, (int) Math.ceil(v1 * h)));
            int stepX = Math.max(1, (x1 - x0) / 48);
            int stepY = Math.max(1, (y1 - y0) / 48);
            for (int y = y0; y < y1; y += stepY) {
                for (int x = x0; x < x1; x += stepX) {
                    if ((image.getColorArgb(x, y) >>> 24) > 24) {
                        return true;
                    }
                }
            }
            return x0 >= w || y0 >= h;
        };
    }

    private static String stripBom(String text) {
        return !text.isEmpty() && text.charAt(0) == '﻿' ? text.substring(1) : text;
    }
}
