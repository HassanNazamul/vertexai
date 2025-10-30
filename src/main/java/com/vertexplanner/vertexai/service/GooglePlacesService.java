package com.vertexplanner.vertexai.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.vertexplanner.vertexai.model.gemini.PlaceDetails;

import reactor.core.publisher.Mono;

@Service
public class GooglePlacesService {

    private final WebClient webClient;
    private final String apiKey;

    public GooglePlacesService(WebClient.Builder webClientBuilder,
            @Value("${google.places.api.key}") String apiKey) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_PLACES_API_KEY_HERE")) {
            System.err.println("⚠️ WARN: Google Places API Key is missing in application.properties!");
        }

        this.apiKey = apiKey;
        this.webClient = webClientBuilder
                .baseUrl("https://places.googleapis.com/v1/places")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Goog-Api-Key", apiKey)
                .build();
    }

    /**
     * Calls Google Places "Text Search (New)" API and fetches details
     * including rating, price level, phone number, website, and photos.
     */
    public Mono<PlaceDetails> findPlaceDetails(String textQuery) {
        String fieldMask = String.join(",",
                "places.id",
                "places.displayName",
                "places.formattedAddress",
                "places.location",
                "places.websiteUri",
                "places.rating",
                "places.photos",
                "places.priceLevel",
                "places.nationalPhoneNumber");

        return this.webClient.post()
                .uri(":searchText")
                .header("X-Goog-FieldMask", fieldMask)
                .bodyValue(Map.of(
                        "textQuery", textQuery,
                        "maxResultCount", 1))
                .retrieve()
                .bodyToMono(JsonNode.class)
                // Optional: log the raw JSON response for debugging
                .doOnNext(json -> System.out.println("DEBUG: Places API response -> " + json))
                .map(this::parsePlaceDetailsFromJsonNode)
                .onErrorResume(e -> {
                    System.err.println("❌ Error calling Places API for query [" + textQuery + "]: " + e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Parses the JSON response into our custom PlaceDetails object.
     */
    private PlaceDetails parsePlaceDetailsFromJsonNode(JsonNode rootNode) {
        if (rootNode == null || !rootNode.has("places") || !rootNode.get("places").isArray()
                || rootNode.get("places").isEmpty()) {
            System.err.println("⚠️ WARN: No 'places' data found in API response.");
            return null;
        }

        JsonNode placeNode = rootNode.get("places").get(0);
        if (placeNode == null)
            return null;

        PlaceDetails details = new PlaceDetails();

        details.setPlaceId(safeGetText(placeNode, "id"));
        details.setFormattedAddress(safeGetText(placeNode, "formattedAddress"));
        details.setWebsite(safeGetText(placeNode, "websiteUri"));
        details.setRating(safeGetDouble(placeNode, "rating"));
        details.setPhoneNumber(safeGetText(placeNode, "nationalPhoneNumber"));
        details.setPriceLevel(safeGetText(placeNode, "priceLevel"));

        // Location
        JsonNode locationNode = placeNode.path("location");
        if (!locationNode.isMissingNode()) {
            details.setLat(safeGetDouble(locationNode, "latitude"));
            details.setLng(safeGetDouble(locationNode, "longitude"));
        }

        // Photos
        details.setPhotoUrls(parsePhotoUrls(placeNode.path("photos")));

        return (details.getPlaceId() != null) ? details : null;
    }

    /**
     * Builds valid photo URLs from the 'photos' array.
     */
    private List<String> parsePhotoUrls(JsonNode photosNode) {
        List<String> photoUrls = new ArrayList<>();
        if (photosNode != null && photosNode.isArray()) {
            for (JsonNode photo : photosNode) {
                String photoName = safeGetText(photo, "name");
                if (photoName != null && !photoName.isEmpty()) {
                    // ✅ Correct URL construction
                    String photoUrl = String.format(
                            "https://places.googleapis.com/v1/%s/media?maxHeightPx=800&key=%s",
                            photoName,
                            this.apiKey);
                    photoUrls.add(photoUrl);
                }
            }
        }
        // Limit to 3 photos for performance
        return photoUrls.stream().limit(3).toList();
    }

    // --- Safe JSON access helpers ---
    private String safeGetText(JsonNode node, String fieldName) {
        return Optional.ofNullable(node)
                .map(n -> n.get(fieldName))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .orElse(null);
    }

    private double safeGetDouble(JsonNode node, String fieldName) {
        return Optional.ofNullable(node)
                .map(n -> n.get(fieldName))
                .filter(JsonNode::isNumber)
                .map(JsonNode::asDouble)
                .orElse(0.0);
    }
}
