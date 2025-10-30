package com.vertexplanner.vertexai.model.gemini;

import lombok.Data;

@Data
public class Weather {

    private int temperature; // e.g., in Celsius
    private String condition; // e.g., "Sunny", "Cloudy"
}
