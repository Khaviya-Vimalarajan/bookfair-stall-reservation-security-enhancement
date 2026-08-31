package com.bookfair.Stall_Reservation.dto.event;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class CreateEventRequest {

    @NotBlank(message = "Event name is required")
    @Size(max = 150, message = "Event name must not exceed 150 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @NotNull
    @FutureOrPresent(message = "Event date cannot be in the past")
    private LocalDateTime eventDate;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Valid
    @NotEmpty(message = "At least one stall is required")
    private List<CreateStallRequest> stalls;

    // getters and setters

    public @Size(max = 1000, message = "Description must not exceed 1000 characters") String getDescription() {
        return description;
    }

    public void setDescription(@Size(max = 1000, message = "Description must not exceed 1000 characters") String description) {
        this.description = description;
    }

    public @FutureOrPresent(message = "Event date cannot be in the past") LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(@FutureOrPresent(message = "Event date cannot be in the past") LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public @Size(max = 500, message = "Image URL must not exceed 500 characters") String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Size(max = 500, message = "Image URL must not exceed 500 characters") String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public @NotBlank(message = "Location is required") @Size(max = 255, message = "Location must not exceed 255 characters") String getLocation() {
        return location;
    }

    public void setLocation(@NotBlank(message = "Location is required") @Size(max = 255, message = "Location must not exceed 255 characters") String location) {
        this.location = location;
    }

    public @NotBlank(message = "Event name is required") @Size(max = 150, message = "Event name must not exceed 150 characters") String getName() {
        return name;
    }

    public void setName(@NotBlank(message = "Event name is required") @Size(max = 150, message = "Event name must not exceed 150 characters") String name) {
        this.name = name;
    }

    public List<CreateStallRequest> getStalls() {
        return stalls;
    }

    public void setStalls(List<CreateStallRequest> stalls) {
        this.stalls = stalls;
    }
}