package com.vertexplanner.vertexai.repositroy;


import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

import com.vertexplanner.vertexai.model.entity.Trip;

import reactor.core.publisher.Flux;

/**
 * This is the repository interface.
 * We extend ReactiveMongoRepository because:
 * 1. We're using MongoDB.
 * 2. We're using a reactive (non-blocking) application.
 *
 * It works with the <Trip, String> domain model.
 * "Trip" = The @Document class to manage.
 * "String" = The data type of the @Id in the Trip class.
 */
@Repository
public interface TripRepository extends ReactiveMongoRepository<Trip, String> {

    /**
     * This is a custom "finder" method.
     * By just declaring this method name, Spring Data MongoDB will
     * automatically write the code to find all trips that match
     * a specific 'userId'.
     *
     * We return a "Flux<Trip>" which is the reactive equivalent
     * of a "List<Trip>".
     *
     * @param userId The Firebase UID of the user.
     * @return A Flux (stream) of all trips belonging to that user.
     */
    Flux<Trip> findByUserId(String userId);

}
