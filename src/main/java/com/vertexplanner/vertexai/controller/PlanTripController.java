package com.vertexplanner.vertexai.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vertexplanner.vertexai.model.gemini.Day;
import com.vertexplanner.vertexai.model.gemini.TripPlan; // Your POJO
import com.vertexplanner.vertexai.service.TripPlanService; // Your new service

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

/**
 * This is the REST Controller that exposes our AI service to the web.
 * It will listen on the path /api/v1/plan
 */
@RestController
@RequestMapping("/api/v1/plan")
// --- NEW ---
// This groups all endpoints in this controller under a single "tag" in the
// Swagger UI
@Tag(name = "Trip Planning", description = "APIs for generating travel itineraries using AI")
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
        @Operation(summary = "Generate a new trip plan", description = "Generates a complete multi-day trip plan from a single text prompt.")
        @ApiResponse(responseCode = "200", description = "Successfully generated trip plan")
        @PostMapping("/generate")
        public Mono<TripPlan> generatePlan(
                        @RequestBody PlanRequest request) {

                // Call your service with the prompt from the request
                return tripPlanService.generateTripPlan(request.prompt());
        }

        /**
         * --- NEW ENDPOINT ---
         * Handles POST requests to /api/v1/plan/options/day
         * Expects a JSON body like:
         * {
         * "location": "Rome",
         * "dayNumber": 2,
         * "preferences": "art, low budget",
         * "numberOfOptions": 3
         * }
         *
         * @return A Mono<List<Day>>
         */

        // --- SAFE ANNOTATIONS ---
        @Operation(summary = "Get alternative options for a single day", description = "Generates a list of alternative 'Day' plans for a specific day.")
        @ApiResponse(responseCode = "200", description = "Successfully generated daily options")
        @PostMapping("/options/day")
        public Mono<List<Day>> generateDailyOptions(
                        @RequestBody DailyOptionsRequest request) {

                System.out.println("Received request for daily options: " + request);

                // This calls the new service method we created in Step 2
                return tripPlanService.generateDailyOptions(request);
        }

        /**
         * A simple Java "record" to represent the incoming request JSON.
         * Spring Boot will automatically convert the JSON into this object.
         */
        public record PlanRequest(String prompt) {
        }

}
