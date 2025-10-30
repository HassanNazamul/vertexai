package com.vertexplanner.vertexai.model.gemini;

import lombok.Data;

@Data
public class Activity {
    private String name;
    private double price;
    private String duration; // e.g., "2 hours", "45 minutes"

    // --- ADD THIS LINE ---
    // This will hold the Google Places API info
    private PlaceDetails placeDetails;
}
