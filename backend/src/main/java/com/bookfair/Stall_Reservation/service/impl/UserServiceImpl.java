package com.bookfair.Stall_Reservation.service.impl;

import com.bookfair.Stall_Reservation.entity.User;
import com.bookfair.Stall_Reservation.repository.UserRepository;
import com.bookfair.Stall_Reservation.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import com.bookfair.Stall_Reservation.dto.profile.ProfileUpdateRequest;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Map<String, Object> getProfile(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return null;
        return Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "businessName", user.getBusinessName() != null ? user.getBusinessName() : "");
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, ProfileUpdateRequest updates) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (updates.getName() != null)
            user.setName(updates.getName());
        if (updates.getPhone() != null)
            user.setPhone(updates.getPhone());
        if (updates.getBusinessName() != null)
            user.setBusinessName(updates.getBusinessName());
        userRepository.save(user);
    }
}
