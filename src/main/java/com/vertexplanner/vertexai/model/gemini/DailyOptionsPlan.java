package com.vertexplanner.vertexai.model.gemini;

import java.util.List;

import lombok.Data;

@Data
public class DailyOptionsPlan {

    // The AI will be instructed to put its list of 'Day' objects here
    private List<Day> dailyOptions;
}