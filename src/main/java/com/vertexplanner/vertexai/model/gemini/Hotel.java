package com.vertexplanner.vertexai.model.gemini;

import lombok.Data;

@Data
public class Hotel {

    private String hotelName;
    private String location;
    private double pricePerNight;

    private PlaceDetails placeDetails;
}
