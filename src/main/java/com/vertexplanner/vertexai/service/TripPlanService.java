package com.vertexplanner.vertexai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import com.vertexplanner.vertexai.controller.DailyOptionsRequest;
import com.vertexplanner.vertexai.model.gemini.Activity;
import com.vertexplanner.vertexai.model.gemini.DailyOptionsPlan;
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

                *** HOTEL INSTRUCTIONS ***
                - You MUST suggest a DIFFERENT hotel for each day of the trip.
                - Do NOT repeat the same hotel name for consecutive days unless specifically asked.
                - The goal is to give the user a variety of options to choose from.

                - Each day MUST have a list of 'activities'.
                - Each 'activity' MUST have a name, an estimated price, and a duration.
                - Be specific with names (e.g., "Louvre Museum", not just "a museum").

                You MUST return your response in the following JSON format:
                %s
                """.formatted(formatInstructions);

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
     * --- NEW PUBLIC METHOD ---
     * Generates a list of alternative Day plans.
     */
    public Mono<List<Day>> generateDailyOptions(DailyOptionsRequest request) {

        // 1. Create an output converter for our new wrapper class (from Step 1)
        var outputConverter = new BeanOutputConverter<>(DailyOptionsPlan.class);
        String formatInstructions = outputConverter.getFormat();

        String systemPrompt = """
                You are a travel planner. Your job is to create several
                alternative, detailed, and inspiring travel plans for a single day.

                - You MUST generate exactly %d different options.
                - Each option MUST be a complete 'Day' object.
                - Each 'Day' object must have its own weather, a single hotel,
                  and a list of activities.

                *** CRITICAL HOTEL INSTRUCTION ***
                - Each of the generated options MUST feature a DIFFERENT hotel.
                - Do NOT repeat the same hotel across the different options.
                - The goal is to provide variety (e.g., different styles or locations).

                - All plans should be for Day %d in %s.
                - The user's preferences are: %s.

                You MUST return your response in the following JSON format:
                %s
                """.formatted(
                request.numberOfOptions(),
                request.dayNumber(),
                request.location(),
                request.preferences(),
                formatInstructions);

        // 3. Create a simple user prompt (the system prompt does most of the work)
        String userPrompt = "Generate %d options for Day %d in %s."
                .formatted(request.numberOfOptions(), request.dayNumber(), request.location());

        // 4. Call the AI
        return Mono.fromCallable(() -> chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(outputConverter) // This returns a Mono<DailyOptionsPlan>
        )
                .flatMap(dailyOptionsPlan -> {
                    if (dailyOptionsPlan == null || dailyOptionsPlan.getDailyOptions() == null) {
                        return Mono.just(List.<Day>of()); // Return an empty list if AI fails
                    }
                    // 5. Enrich the list of days and return it
                    // We will write 'enrichDays' in the next action
                    return this.enrichDays(dailyOptionsPlan.getDailyOptions(), request.location())
                            .thenReturn(dailyOptionsPlan.getDailyOptions()); // Return the enriched list
                });
    }

    /**
     * --- REFACTORED ---
     * This method now just calls the new reusable 'enrichDays' method.
     */
    private Mono<TripPlan> enrichPlanWithPlaceDetails(TripPlan plan) {
        if (plan == null || plan.getDays() == null) {
            System.err.println("WARN: AI returned null plan or null days list. Skipping enrichment.");
            return Mono.justOrEmpty(plan);
        }

        // Call the new reusable enrichment method (which we will create next)
        // and pass it the plan's days and location.
        return enrichDays(plan.getDays(), plan.getLocation())
                .thenReturn(plan); // After enrichment is done, return the (now mutated) plan
    }

    /**
     * --- NEW REUSABLE METHOD ---
     * This method contains the enrichment logic extracted from the original
     * 'enrichPlanWithPlaceDetails'. It can enrich ANY list of Day objects.
     */
    private Mono<List<Day>> enrichDays(List<Day> days, String locationHint) {

        List<Mono<Void>> enrichmentTasks = new ArrayList<>();

        for (Day day : days) {
            // Enrich the Hotel
            if (day.getHotel() != null && day.getHotel().getHotelName() != null
                    && !day.getHotel().getHotelName().isBlank()) {
                String hotelQuery = day.getHotel().getHotelName() + ", " + locationHint;

                Mono<Void> hotelTask = placesService.findPlaceDetails(hotelQuery)
                        .doOnNext(placeDetails -> {
                            if (placeDetails != null) {
                                day.getHotel().setPlaceDetails(placeDetails);
                            } else {
                                System.err.println("WARN: No place details found for hotel: " + hotelQuery);
                            }
                        })
                        .then();
                enrichmentTasks.add(hotelTask);
            }

            // Enrich Activities
            if (day.getActivities() != null) {
                for (Activity activity : day.getActivities()) {
                    if (activity != null && activity.getName() != null && !activity.getName().isBlank()) {
                        String activityQuery = activity.getName() + ", " + locationHint;

                        Mono<Void> activityTask = placesService.findPlaceDetails(activityQuery)
                                .doOnNext(placeDetails -> {
                                    if (placeDetails != null) {
                                        activity.setPlaceDetails(placeDetails);
                                    } else {
                                        System.err
                                                .println("WARN: No place details found for activity: " + activityQuery);
                                    }
                                })
                                .then();
                        enrichmentTasks.add(activityTask);
                    }
                }
            }
        } // End of loop

        if (enrichmentTasks.isEmpty()) {
            return Mono.just(days); // No enrichment needed
        }

        // Execute all tasks in parallel and return the (mutated) list when done
        System.out.println("INFO: Starting enrichment for " + enrichmentTasks.size() + " items...");
        return Mono.when(enrichmentTasks)
                .thenReturn(days)
                .doOnSuccess(p -> System.out.println("INFO: Enrichment complete."));
    }

}// End of TripPlanService.java