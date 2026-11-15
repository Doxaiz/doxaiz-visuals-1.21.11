package com.lowcode.svintusclient.client.holyworld;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class HolyWorldApiService {

    public static final String BASE_URL = "https://api.holyworld.me/v1";
    private static final Gson GSON = new Gson();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "holyworld-api-worker");
        t.setDaemon(true);
        return t;
    });

    private static boolean pollingStarted = false;

    // Thread-safe API state
    public static volatile boolean isApiOnline = false;
    public static volatile long lastUpdateTime = 0L;

    // Coin trades
    public static volatile double latestCoinRate = 27_500_000.0;
    public static volatile double coinMedianRate = 27_000_000.0;
    public static volatile double coinPriceChangePercent = 0.0;
    public static volatile String coinRateFormatted = "27.5M";

    // Wipe info
    public static volatile int daysSinceWipe = 14;
    public static volatile String currentAnarchyServer = "an12";

    // Events list
    public record HwEvent(String name, String location, String rare) {}
    private static final List<HwEvent> EVENTS = Collections.synchronizedList(new ArrayList<>());

    // Servers map: ID -> display name
    public static final Map<String, String> SERVERS = new ConcurrentHashMap<>();

    // Votings list
    public record HwVoting(String serverId, String title, int votes) {}
    private static final List<HwVoting> VOTINGS = Collections.synchronizedList(new ArrayList<>());

    private HolyWorldApiService() {}

    public static synchronized void startPolling() {
        if (pollingStarted) return;
        pollingStarted = true;

        SCHEDULER.scheduleWithFixedDelay(HolyWorldApiService::pollOnce, 0, 30, TimeUnit.SECONDS);
    }

    private static void pollOnce() {
        try {
            // 1. Health check
            String healthJson = fetch("/health");
            if (healthJson != null && healthJson.contains("ok")) {
                isApiOnline = true;
            }

            // 2. Events
            String eventsJson = fetch("/events");
            if (eventsJson != null) {
                parseEvents(eventsJson);
            }

            // 3. Coin trades
            String coinsJson = fetch("/coins-trades?limit=10");
            if (coinsJson != null) {
                parseCoins(coinsJson);
            }

            // 4. Servers
            String serversJson = fetch("/servers");
            if (serversJson != null) {
                parseServers(serversJson);
            }

            // 5. Votings
            String votingsJson = fetch("/votings");
            if (votingsJson != null) {
                parseVotings(votingsJson);
            }

            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception ignored) {
            isApiOnline = false;
        }
    }

    private static String fetch(String endpoint) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "DoxaizVisuals/1.2.0")
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void parseEvents(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return;
            JsonObject obj = el.getAsJsonObject();

            List<HwEvent> parsed = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String server = entry.getKey();
                if (entry.getValue().isJsonArray()) {
                    JsonArray arr = entry.getValue().getAsJsonArray();
                    for (JsonElement item : arr) {
                        if (!item.isJsonObject()) continue;
                        JsonObject evObj = item.getAsJsonObject();
                        String id = evObj.has("id") ? evObj.get("id").getAsString() : "EVENT";
                        String rare = "Обычный";
                        String name = id;
                        if (evObj.has("metadata") && evObj.get("metadata").isJsonObject()) {
                            JsonObject meta = evObj.getAsJsonObject("metadata");
                            if (meta.has("displayName")) name = meta.get("displayName").getAsString();
                            if (meta.has("rare")) rare = meta.get("rare").getAsString();
                        }
                        parsed.add(new HwEvent(name, server, rare));
                    }
                }
            }
            synchronized (EVENTS) {
                EVENTS.clear();
                EVENTS.addAll(parsed);
            }
        } catch (Exception ignored) {}
    }

    private static void parseCoins(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonArray()) return;
            JsonArray arr = el.getAsJsonArray();
            if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                JsonObject latest = arr.get(0).getAsJsonObject();
                if (latest.has("rate")) {
                    double r = latest.get("rate").getAsDouble();
                    latestCoinRate = r;
                    if (r >= 1_000_000.0) {
                        coinRateFormatted = String.format("%.1fM", r / 1_000_000.0);
                    } else if (r >= 1_000.0) {
                        coinRateFormatted = String.format("%.1fK", r / 1_000.0);
                    } else {
                        coinRateFormatted = String.format("%.0f", r);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void parseServers(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject()) return;
            JsonObject obj = el.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    SERVERS.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception ignored) {}
    }

    private static void parseVotings(String json) {
        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonArray()) return;
            JsonArray arr = el.getAsJsonArray();
            List<HwVoting> list = new ArrayList<>();
            for (JsonElement item : arr) {
                if (!item.isJsonObject()) continue;
                JsonObject obj = item.getAsJsonObject();
                String server = obj.has("serverId") ? obj.get("serverId").getAsString() : "";
                String title = obj.has("id") ? obj.get("id").getAsString() : "Голосование";
                int votes = obj.has("votes") ? obj.get("votes").getAsInt() : 0;
                list.add(new HwVoting(server, title, votes));
            }
            synchronized (VOTINGS) {
                VOTINGS.clear();
                VOTINGS.addAll(list);
            }
        } catch (Exception ignored) {}
    }

    public static List<HwEvent> getEvents() {
        synchronized (EVENTS) {
            return new ArrayList<>(EVENTS);
        }
    }

    public static List<HwVoting> getVotings() {
        synchronized (VOTINGS) {
            return new ArrayList<>(VOTINGS);
        }
    }
}
