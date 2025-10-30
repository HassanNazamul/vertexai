package com.vertexplanner.vertexai.model.gemini;

import java.util.List;

import lombok.Data;

@Data
public class PlaceDetails {
    private String placeId; // The Google Place ID
    private String formattedAddress;
    private double lat; // Latitude
    private double lng; // Longitude
    private String website;
    private double rating; // e.g., 4.5
    private String phoneNumber;
    private String priceLevel;
    private List<String> photoUrls; // We'll store the final image URLs here
}