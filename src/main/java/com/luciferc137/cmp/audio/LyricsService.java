package com.luciferc137.cmp.audio;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service for fetching lyrics from online sources.
 * Uses the lrclib.net API (free and reliable).
 */
public class LyricsService {

    private static final String API_URL = "https://lrclib.net/api/search";
    private static final int TIMEOUT_SECONDS = 15;

    // Shared HttpClient instance (thread-safe, reusable)
    private static final HttpClient httpClient;

    static {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Result of a lyrics fetch operation.
     */
    public static class LyricsResult {
        private final boolean success;
        private final String lyrics;
        private final String errorMessage;

        private LyricsResult(boolean success, String lyrics, String errorMessage) {
            this.success = success;
            this.lyrics = lyrics;
            this.errorMessage = errorMessage;
        }

        public static LyricsResult success(String lyrics) {
            return new LyricsResult(true, lyrics, null);
        }

        public static LyricsResult error(String message) {
            return new LyricsResult(false, null, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getLyrics() {
            return lyrics;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Fetches lyrics asynchronously for the given artist and title.
     *
     * @param artist The artist name
     * @param title  The song title
     * @return A CompletableFuture containing the result
     */
    public static CompletableFuture<LyricsResult> fetchLyricsAsync(String artist, String title) {
        return CompletableFuture.supplyAsync(() -> fetchLyrics(artist, title));
    }

    /**
     * Fetches lyrics synchronously for the given artist and title.
     *
     * @param artist The artist name
     * @param title  The song title
     * @return The result containing lyrics or error message
     */
    public static LyricsResult fetchLyrics(String artist, String title) {
        if (artist == null || artist.trim().isEmpty()) {
            return LyricsResult.error("Artist name is required");
        }
        if (title == null || title.trim().isEmpty()) {
            return LyricsResult.error("Song title is required");
        }

        try {
            // Build search query: ?artist_name=X&track_name=Y
            String query = "?artist_name=" + URLEncoder.encode(artist.trim(), StandardCharsets.UTF_8)
                    + "&track_name=" + URLEncoder.encode(title.trim(), StandardCharsets.UTF_8);
            String urlString = API_URL + query;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "CMP-MusicPlayer/1.0 (https://github.com/music-player)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int responseCode = response.statusCode();

            if (responseCode == 200) {
                String jsonResponse = response.body();
                String lyrics = extractLyricsFromLrclib(jsonResponse);

                if (lyrics != null && !lyrics.trim().isEmpty()) {
                    return LyricsResult.success(lyrics.trim());
                } else {
                    return LyricsResult.error("No lyrics found for \"" + title + "\" by " + artist);
                }
            } else if (responseCode == 404) {
                return LyricsResult.error("No lyrics found for \"" + title + "\" by " + artist);
            } else {
                return LyricsResult.error("Server error (HTTP " + responseCode + ")");
            }

        } catch (java.net.http.HttpTimeoutException e) {
            return LyricsResult.error("Request timed out. Please try again.");
        } catch (java.net.ConnectException e) {
            return LyricsResult.error("No internet connection");
        } catch (javax.net.ssl.SSLHandshakeException e) {
            System.err.println("SSL Handshake failed: " + e.getMessage());
            return LyricsResult.error("SSL connection error: " + e.getMessage());
        } catch (javax.net.ssl.SSLException e) {
            System.err.println("SSL error: " + e.getMessage());
            return LyricsResult.error("Secure connection failed: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.err.println("IO error: " + e.getMessage());
            return LyricsResult.error("Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return LyricsResult.error("Request was interrupted");
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getClass().getName() + ": " + e.getMessage());
            return LyricsResult.error("Error: " + e.getMessage());
        }
    }

    /**
     * Extracts plain lyrics from lrclib.net JSON array response.
     * The API returns an array of results, we take the first one with plainLyrics.
     */
    private static String extractLyricsFromLrclib(String json) {
        if (json == null || json.trim().isEmpty() || json.trim().equals("[]")) {
            return null;
        }

        // Look for "plainLyrics" field (preferred) or "syncedLyrics"
        String lyrics = extractJsonStringField(json, "plainLyrics");

        if (lyrics == null || lyrics.trim().isEmpty()) {
            // Fallback to synced lyrics and remove timestamps
            lyrics = extractJsonStringField(json, "syncedLyrics");
            if (lyrics != null) {
                lyrics = removeLrcTimestamps(lyrics);
            }
        }

        return lyrics;
    }

    /**
     * Extracts a string field value from JSON.
     */
    private static String extractJsonStringField(String json, String fieldName) {
        String searchKey = "\"" + fieldName + "\":\"";
        int startIndex = json.indexOf(searchKey);

        if (startIndex == -1) {
            // Try with space after colon
            searchKey = "\"" + fieldName + "\": \"";
            startIndex = json.indexOf(searchKey);
        }
        
        // Check for null value
        String nullCheck = "\"" + fieldName + "\":null";
        if (json.contains(nullCheck) || json.contains("\"" + fieldName + "\": null")) {
            return null;
        }

        if (startIndex == -1) {
            return null;
        }

        startIndex += searchKey.length();
        
        // Find the end of the string value
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (escaped) {
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    default -> value.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
            }
        }

        return value.toString();
    }

    /**
     * Removes LRC timestamps from synced lyrics.
     * Timestamps look like [00:15.50]
     */
    private static String removeLrcTimestamps(String syncedLyrics) {
        if (syncedLyrics == null) return null;
        // Remove patterns like [00:00.00] or [0:00.00]
        return syncedLyrics.replaceAll("\\[\\d{1,2}:\\d{2}\\.\\d{2}\\]\\s*", "");
    }
}

