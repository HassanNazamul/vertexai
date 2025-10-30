package com.vertexplanner.vertexai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import com.vertexplanner.vertexai.model.gemini.Activity;
import com.vertexplanner.vertexai.model.gemini.Day;
import com.vertexplanner.vertexai.model.gemini.TripPlan;

import reactor.core.publisher.Mono;

@Service
public class TripPlanService {

    private final ChatClient chatClient;
    // --- 1. INJECT THE NEW SERVICE ---
    private final GooglePlacesService placesService;

    public TripPlanService(ChatClient.Builder chatClientBuilder,
            // --- 2. ADD TO CONSTRUCTOR ---
            GooglePlacesService placesService) { // <-- Inject GooglePlacesService
        this.chatClient = chatClientBuilder.build();
        this.placesService = placesService; // <-- Store it
    }

    /**
     * This is the main method that calls the AI and then enriches the result.
     * It takes a plain text prompt and returns a "Mono" (a reactive object)
     * containing your structured AND enriched TripPlan.
     *
     * @param userPrompt The user's request, e.g., "Plan a 5 day trip to Paris"
     * @return A Mono that will emit the enriched TripPlan when ready.
     */
    public Mono<TripPlan> generateTripPlan(String userPrompt) {

        // 1. Create an output converter for your specific Java class
        var outputConverter = new BeanOutputConverter<>(TripPlan.class);

        // 2. Get the "format instructions" from the converter.
        String formatInstructions = outputConverter.getFormat();

        // 3. Create a system prompt. This gives the AI its main role and instructions.
        String systemPrompt = """
                You are a master travel planner. Your job is to create a detailed,
                logical, and inspiring travel itinerary based on the user's request.

                - You must infer the number of days from the start and end date.
                - Each day in the 'days' list MUST have its own daily weather forecast.
                - Each day MUST have a *single* hotel suggestion (the 'hotel' object).
                - Each day MUST have a list of 'activities'.
                - Each 'activity' MUST have a name, an estimated price, and a duration.
                - Be specific with names (e.g., "Louvre Museum", not just "a museum").

                You MUST return your response in the following JSON format:
                %s
                """.formatted(formatInstructions); // We insert the instructions here.
        // --- 4. CORRECTED REACTIVE CHAIN ---
        // Step A: Wrap the potentially blocking AI call in Mono.fromCallable
        return Mono.fromCallable(() -> chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(outputConverter) // This call might block
        ) // Step B: The result of fromCallable is a Mono<TripPlan>
          // Step C: Now, flatMap works correctly on the Mono<TripPlan>
                .flatMap(this::enrichPlanWithPlaceDetails);
    }

    /**
     * This private method takes the basic plan from the AI and enriches it
     * by calling the Google Places API for each relevant item (hotels, activities).
     * It uses reactive patterns to make these calls efficiently in parallel.
     *
     * @param plan The basic TripPlan returned by the Gemini AI.
     * @return A Mono that will emit the same TripPlan object, but now enriched
     *         with PlaceDetails where available.
     */
    private Mono<TripPlan> enrichPlanWithPlaceDetails(TripPlan plan) {
        // Basic validation: If the plan or its days are null, return it as is.
        if (plan == null || plan.getDays() == null) {
            System.err.println("WARN: AI returned null plan or null days list. Skipping enrichment.");
            return Mono.justOrEmpty(plan);
        }

        // Create a list to hold all the individual Mono tasks for fetching place
        // details.
        List<Mono<Void>> enrichmentTasks = new ArrayList<>();
        String locationHint = plan.getLocation(); // Use trip location as context for better search results

        // Iterate through each day in the plan
        for (Day day : plan.getDays()) {

            // Enrich the Hotel for the day, if it exists
            if (day.getHotel() != null && day.getHotel().getHotelName() != null
                    && !day.getHotel().getHotelName().isBlank()) {
                String hotelQuery = day.getHotel().getHotelName() + ", " + locationHint;

                // Create a Mono task to find place details for the hotel
                Mono<Void> hotelTask = placesService.findPlaceDetails(hotelQuery)
                        .doOnNext(placeDetails -> { // Use doOnNext for the side-effect of setting details
                            if (placeDetails != null) {
                                day.getHotel().setPlaceDetails(placeDetails);
                            } else {
                                System.err.println("WARN: No place details found for hotel: " + hotelQuery);
                            }
                        })
                        .then(); // Convert Mono<PlaceDetails> to Mono<Void> for joining with Mono.when
                enrichmentTasks.add(hotelTask); // Add the task to our list
            } else if (day.getHotel() != null) {
                System.err.println("WARN: Hotel name missing for day " + day.getDayNumber() + ". Skipping enrichment.");
            }

            // Enrich Activities for the day, if they exist
            if (day.getActivities() != null) {
                for (Activity activity : day.getActivities()) {
                    if (activity != null && activity.getName() != null && !activity.getName().isBlank()) {
                        String activityQuery = activity.getName() + ", " + locationHint;

                        // Create a Mono task to find place details for the activity
                        Mono<Void> activityTask = placesService.findPlaceDetails(activityQuery)
                                .doOnNext(placeDetails -> {
                                    if (placeDetails != null) {
                                        activity.setPlaceDetails(placeDetails);
                                    } else {
                                        System.err
                                                .println("WARN: No place details found for activity: " + activityQuery);
                                    }
                                })
                                .then(); // Convert to Mono<Void>
                        enrichmentTasks.add(activityTask); // Add the task to our list
                    } else if (activity != null) {
                        System.err.println(
                                "WARN: Activity name missing for day " + day.getDayNumber() + ". Skipping enrichment.");
                    }
                }
            }
        } // End of loop through days

        // If no enrichment tasks were created, just return the original plan
        if (enrichmentTasks.isEmpty()) {
            return Mono.just(plan);
        }

        // --- 5. EXECUTE ALL TASKS IN PARALLEL ---
        // Mono.when takes a list of Monos (in our case, Mono<Void>) and runs them
        // concurrently.
        // It returns a single Mono<Void> that completes only when ALL the input Monos
        // have completed.
        // .thenReturn(plan) is crucial: after all enrichment tasks are done, it emits
        // the original
        // 'plan' object (which has been modified by the .doOnNext side-effects)
        // downstream.
        System.out.println("INFO: Starting enrichment for " + enrichmentTasks.size() + " items...");
        return Mono.when(enrichmentTasks)
                .thenReturn(plan) // Return the modified plan object after all tasks finish
                .doOnSuccess(p -> System.out.println("INFO: Enrichment complete."));
    }
}