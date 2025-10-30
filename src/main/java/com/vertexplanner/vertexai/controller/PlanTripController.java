package com.vertexplanner.vertexai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vertexplanner.vertexai.model.gemini.TripPlan; // Your POJO
import com.vertexplanner.vertexai.service.TripPlanService; // Your new service

import reactor.core.publisher.Mono; // Import Mono

/**
 * This is the REST Controller that exposes our AI service to the web.
 * It will listen on the path /api/v1/plan
 */
@RestController
@RequestMapping("/api/v1/plan")
public class PlanTripController {

    private final TripPlanService tripPlanService;

    // Spring Boot injects your service here
    public PlanTripController(TripPlanService tripPlanService) {
        this.tripPlanService = tripPlanService;
    }

    /**
     * This method handles POST requests to /api/v1/plan/generate
     * It expects a simple JSON request body like:
     * {
     * "prompt": "Plan a 3 day trip to Rome"
     * }
     *
     * It returns a Mono<GeminiTripPlan>, which Spring Web will handle
     * automatically, sending the JSON response once it's ready.
     */
    @PostMapping("/generate")
    public Mono<TripPlan> generatePlan(
            @RequestBody PlanRequest request) {

        // Call your service with the prompt from the request
        return tripPlanService.generateTripPlan(request.prompt());
    }

    /**
     * A simple Java "record" to represent the incoming request JSON.
     * Spring Boot will automatically convert the JSON into this object.
     */
    public record PlanRequest(String prompt) {
    }

}
