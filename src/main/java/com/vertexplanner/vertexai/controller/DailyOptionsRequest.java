package com.vertexplanner.vertexai.controller;

/**
 * Represents the request from the frontend for daily options.
 * e.g., { "location": "Rome", "dayNumber": 2, "preferences": "art, low budget", "numberOfOptions": 3 }
 */
public record DailyOptionsRequest(
    String location,
    int dayNumber,
    String preferences,
    int numberOfOptions
) {
}
