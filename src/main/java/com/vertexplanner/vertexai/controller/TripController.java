package com.vertexplanner.vertexai.controller;


// --- NEW IMPORTS ---
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping; // For DELETE
import org.springframework.web.bind.annotation.GetMapping; // For Query Params
import org.springframework.web.bind.annotation.PathVariable; // For 201 Created
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vertexplanner.vertexai.model.entity.Trip;
import com.vertexplanner.vertexai.model.gemini.TripPlan; // Still needed for the request body
import com.vertexplanner.vertexai.repositroy.TripRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for managing Trip resources.
 * Base path: /api/v1/trips
 */
@RestController
// --- CHANGE #1: Update base path ---
@RequestMapping("/api/v1/trips")
public class TripController { // <-- Renamed class

    private final TripRepository tripRepository;

    public TripController(TripRepository tripRepository) { // <-- Renamed constructor
        this.tripRepository = tripRepository;
    }

    /**
     * Creates a new Trip resource.
     * Endpoint: POST /api/v1/trips
     * @param request The request body containing userId and the plan details.
     * @return A Mono containing the saved Trip entity with a 201 Created status.
     */
    // --- CHANGE #2: Update endpoint path and response status ---
    @PostMapping // No path needed, uses the base path
    @ResponseStatus(HttpStatus.CREATED) // Return HTTP 201 Created on success
    public Mono<Trip> createTrip(@RequestBody SaveTripRequest request) {
        Trip newTrip = buildTripEntity(request.plan(), request.userId());
        return tripRepository.save(newTrip);
    }

    /**
     * Gets all Trip resources for a specific user.
     * Endpoint: GET /api/v1/trips?userId={userId}
     * @param userId The ID of the user whose trips are to be fetched.
     * @return A Flux stream of Trip entities belonging to the user.
     */
    // --- CHANGE #3: Use query parameter for filtering ---
    @GetMapping // No path needed, filtering is done via query parameter
    public Flux<Trip> getTripsForUser(@RequestParam String userId) { // Use @RequestParam
        return tripRepository.findByUserId(userId);
    }

    /**
     * Gets a single Trip resource by its ID.
     * Endpoint: GET /api/v1/trips/{tripId}
     * @param tripId The unique ID of the trip.
     * @return A Mono containing the Trip entity or a 404 Not Found status.
     */
    // --- NEW ENDPOINT (Standard REST) ---
    @GetMapping("/{tripId}")
    public Mono<ResponseEntity<Trip>> getTripById(@PathVariable String tripId) {
        return tripRepository.findById(tripId)
                .map(ResponseEntity::ok) // If found, wrap in ResponseEntity with 200 OK
                .defaultIfEmpty(ResponseEntity.notFound().build()); // If not found, return 404
    }
    
    /**
     * Deletes a Trip resource by its ID.
     * Endpoint: DELETE /api/v1/trips/{tripId}
     * @param tripId The unique ID of the trip to delete.
     * @return A Mono indicating completion with a 204 No Content status.
     */
    // --- NEW ENDPOINT (Standard REST) ---
    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Return HTTP 204 No Content on success
    public Mono<Void> deleteTrip(@PathVariable String tripId) {
        // findById checks if it exists before deleting
        // delete() returns Mono<Void> which signals completion
        return tripRepository.findById(tripId)
               .flatMap(tripRepository::delete); 
               // consider adding error handling if not found
    }


    //TODO: will implement PUT to update the existing trip saved by the user


    // --- Helper method (no changes needed) ---
    private Trip buildTripEntity(TripPlan plan, String userId) {
        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setLocation(plan.getLocation());
        trip.setBudget(plan.getBudget());
        trip.setStartDate(plan.getStartDate());
        trip.setEndDate(plan.getEndDate());
        trip.setNumberOfPeople(plan.getNumberOfPeople());
        trip.setTheme(plan.getTheme());
        trip.setDays(plan.getDays());
        return trip;
    }

    // --- Request Body DTO (no changes needed) ---
    public record SaveTripRequest(String userId, TripPlan plan) {}
}