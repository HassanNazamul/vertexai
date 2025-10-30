package com.vertexplanner.vertexai.model.gemini;


import java.util.List;

import lombok.Data;

/**
 * This is the new top-level object for the AI's response,
 * matching your new requirements.
 */
@Data
public class TripPlan {

    private String location;
    private double budget;
    private String startDate;
    private String endDate;
    private int numberOfPeople;
    private String theme; // e.g., "honeymoon", "historic", "beach"
    
    // This will hold the list of Day objects
    private List<Day> days; 
}
