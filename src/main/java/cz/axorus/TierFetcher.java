package cz.axorus;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stahuje tiery asynchronně a cachuje je (šetří API). */
public class TierFetcher {
    // Neoficiální proxy k MCTiers (formát: /mctiers/player/<nick>). Pokud přestane fungovat, změň tady.
    private static final String ENDPOINT = "https://api.yeahjenni.xyz/mctiers/player/";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    public static String get(String name) {
        String key = name.toLowerCase();
        String cached = CACHE.get(key);
        if (cached != null) return cached;
        CACHE.put(key, "...");
        HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT + name))
                .timeout(Duration.ofSeconds(6)).GET().build();
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(res -> {
            CACHE.put(key, res.statusCode() == 200 ? parse(res.body()) : "Unranked");
        }).exceptionally(e -> { CACHE.put(key, "N/A"); return null; });
        return "...";
    }

    private static String parse(String body) {
        try {
            JsonObject modes = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("gameModes");
            String bestMode = null, bestTier = null;
            int bestScore = -1;
            for (var e : modes.entrySet()) {
                if (!e.getValue().isJsonObject() || !e.getValue().getAsJsonObject().has("tier")) continue;
                String tier = e.getValue().getAsJsonObject().get("tier").getAsString();
                int score = score(tier);
                if (score > bestScore) { bestScore = score; bestMode = e.getKey(); bestTier = tier; }
            }
            return bestTier == null ? "Unranked" : bestTier + " (" + bestMode + ")";
        } catch (Exception ex) {
            return "Unranked";
        }
    }

    /** HT1 nejlepší ... LT5 nejhorší. */
    private static int score(String tier) {
        try {
            int n = Integer.parseInt(tier.substring(2));
            return (6 - n) * 2 + (tier.startsWith("HT") ? 1 : 0);
        } catch (Exception e) { return 0; }
    }
}
