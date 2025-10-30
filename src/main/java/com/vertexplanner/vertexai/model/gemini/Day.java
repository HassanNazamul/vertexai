package com.vertexplanner.vertexai.model.gemini;


import java.util.List;

import lombok.Data;

@Data
public class Day {

    private int dayNumber;
    private String date; // e.g., "2025-10-30"
    
    // Weather and Hotel are now INSIDE the Day
    private Weather weather;
    private Hotel hotel;
    
    // A list of activities for this day
    private List<Activity> activities;
}
