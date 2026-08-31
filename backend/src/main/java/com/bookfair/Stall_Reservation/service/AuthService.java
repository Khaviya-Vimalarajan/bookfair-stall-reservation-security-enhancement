package com.bookfair.Stall_Reservation.service;


import com.bookfair.Stall_Reservation.entity.User;

public interface AuthService {




    User getCurrentUser(Long userId);
}
