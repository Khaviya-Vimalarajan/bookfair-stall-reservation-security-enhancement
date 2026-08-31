package com.bookfair.Stall_Reservation.service;
import com.bookfair.Stall_Reservation.dto.profile.ProfileUpdateRequest;
import java.util.Map;

public interface UserService {
    Map<String, Object> getProfile(Long userId);

    void updateProfile(Long userId, ProfileUpdateRequest updates);
}
