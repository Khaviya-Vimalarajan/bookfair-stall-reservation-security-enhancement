package com.bookfair.Stall_Reservation.service.impl;
import com.bookfair.Stall_Reservation.exception.AuthException;
import com.bookfair.Stall_Reservation.entity.User;
import com.bookfair.Stall_Reservation.repository.UserRepository;
import com.bookfair.Stall_Reservation.service.AuthService;
import org.springframework.stereotype.Service;



@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));
    }
}
