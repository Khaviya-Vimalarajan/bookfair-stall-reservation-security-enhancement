package com.bookfair.Stall_Reservation.dto.reservation;

import com.bookfair.Stall_Reservation.enums.StallType;
import com.bookfair.Stall_Reservation.enums.StallSize;
import com.bookfair.Stall_Reservation.enums.BusinessCategory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDate;
import java.util.List;
import com.bookfair.Stall_Reservation.enums.PaymentMethod;
import jakarta.validation.constraints.AssertTrue;

public class CreateBookingRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotEmpty(message = "At least one stall must be selected")
    private List<Long> stallIds;

    @NotEmpty(message = "At least one genre must be selected")
    private List<Long> genreIds;

    @Size(max = 500, message = "Stall description must not exceed 500 characters")
    private String stallDescription;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @NotEmpty(message = "Address is required")
    private String address;

    @NotNull(message = "Stall type is required")
    private StallType stallType;

    @NotNull(message = "Preferred stall size is required")
    private StallSize preferredStallSize;

    @NotNull(message = "Number of stalls required is required")
    @Min(value = 1, message = "At least 1 stall is required")
    private Integer numberOfStallsRequired;

    @NotNull(message = "Business category is required")
    private BusinessCategory businessCategory;

    @NotNull(message = "Reservation date is required")
    @FutureOrPresent(message = "Reservation date cannot be in the past")
    private LocalDate reservationDate;

    @Size(max = 500, message = "Special requirements comments must not exceed 500 characters")
    private String specialRequirements;

    // Getters and Setters
    public StallType getStallType() {
        return stallType;
    }

    public void setStallType(StallType stallType) {
        this.stallType = stallType;
    }

    public StallSize getPreferredStallSize() {
        return preferredStallSize;
    }

    public void setPreferredStallSize(StallSize preferredStallSize) {
        this.preferredStallSize = preferredStallSize;
    }

    public Integer getNumberOfStallsRequired() {
        return numberOfStallsRequired;
    }

    public void setNumberOfStallsRequired(Integer numberOfStallsRequired) {
        this.numberOfStallsRequired = numberOfStallsRequired;
    }

    public BusinessCategory getBusinessCategory() {
        return businessCategory;
    }

    public void setBusinessCategory(BusinessCategory businessCategory) {
        this.businessCategory = businessCategory;
    }

    public LocalDate getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(LocalDate reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getSpecialRequirements() {
        return specialRequirements;
    }

    public void setSpecialRequirements(String specialRequirements) {
        this.specialRequirements = specialRequirements;
    }
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public List<Long> getStallIds() {
        return stallIds;
    }

    public void setStallIds(List<Long> stallIds) {
        this.stallIds = stallIds;
    }

    public List<Long> getGenreIds() {
        return genreIds;
    }

    public void setGenreIds(List<Long> genreIds) {
        this.genreIds = genreIds;
    }

    public String getStallDescription() {
        return stallDescription;
    }

    public void setStallDescription(String stallDescription) {
        this.stallDescription = stallDescription;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @AssertTrue(message = "Account number and bank name are required for bank transfer")
    public boolean isBankTransferDetailsValid() {
        if (paymentMethod != PaymentMethod.BANK_TRANSFER) {
            return true;
        }

        return accountNumber != null &&
                !accountNumber.isBlank() &&
                bankName != null &&
                !bankName.isBlank();
    }
}
