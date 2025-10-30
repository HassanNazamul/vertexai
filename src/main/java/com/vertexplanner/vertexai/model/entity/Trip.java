package com.vertexplanner.vertexai.model.entity;


import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.vertexplanner.vertexai.model.gemini.Day; // Re-using your POJO

import lombok.Data;

/**
 * This class represents a "Trip" document in your MongoDB "trips" collection.
 * It's the only new model class we need to create.
 */
@Data
@Document(collection = "trips") // This tells Spring this is a MongoDB document
public class Trip {

    @Id
    private String id; // MongoDB's unique ID (will be auto-generated)

    // This is the Firebase UID to find all trips for a user
    private String userId;

    // --- All these fields come directly from your AI's response ---
    private String location;
    private double budget;
    private String startDate;
    private String endDate;
    private int numberOfPeople;
    private String theme;
    
    // Spring Data MongoDB will automatically embed the list of Day objects
    // and all *their* nested objects (Weather, Hotel, Activity)
    // inside this one "Trip" document.
    private List<Day> days; 
}
