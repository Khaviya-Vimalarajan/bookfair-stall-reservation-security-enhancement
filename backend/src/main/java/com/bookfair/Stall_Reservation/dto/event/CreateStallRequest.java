package com.bookfair.Stall_Reservation.dto.event;

import com.bookfair.Stall_Reservation.enums.StallSize;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateStallRequest {

    @NotBlank(message = "Stall code is required")
    @Size(max = 50, message = "Stall code must not exceed 50 characters")
    private String stallCode;

    public @NotNull(message = "Stall size is required") StallSize getSize() {
        return size;
    }

    public void setSize(@NotNull(message = "Stall size is required") StallSize size) {
        this.size = size;
    }

    @NotNull(message = "Stall size is required")
    private StallSize size;

    @NotNull(message = "Stall price is required")
    @DecimalMin(value = "0.01", message = "Stall price must be greater than 0")
    private BigDecimal price;

    private boolean blocked;

    private Integer positionX;

    private Integer positionY;

    // getters and setters

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public void setPositionX(Integer positionX) {
        this.positionX = positionX;
    }

    public Integer getPositionY() {
        return positionY;
    }

    public void setPositionY(Integer positionY) {
        this.positionY = positionY;
    }

    public @NotNull(message = "Stall price is required") @DecimalMin(value = "0.01", message = "Stall price must be greater than 0") BigDecimal getPrice() {
        return price;
    }

    public void setPrice(@NotNull(message = "Stall price is required") @DecimalMin(value = "0.01", message = "Stall price must be greater than 0") BigDecimal price) {
        this.price = price;
    }

    public @NotBlank(message = "Stall code is required") @Size(max = 50, message = "Stall code must not exceed 50 characters") String getStallCode() {
        return stallCode;
    }

    public void setStallCode(@NotBlank(message = "Stall code is required") @Size(max = 50, message = "Stall code must not exceed 50 characters") String stallCode) {
        this.stallCode = stallCode;
    }
}