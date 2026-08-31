package com.bookfair.Stall_Reservation.config;

import com.bookfair.Stall_Reservation.entity.*;
import com.bookfair.Stall_Reservation.enums.UserRole;
import com.bookfair.Stall_Reservation.repository.*;
import com.bookfair.Stall_Reservation.service.ContentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner seed(UserRepository userRepository,
                                  GenreRepository genreRepository, ContentService contentService,
                                  EventRepository eventRepository, StallRepository stallRepository) {
        return args -> {
            var existingAdmin = userRepository.findByEmail("admin@bookfair.com");
            if (existingAdmin.isEmpty()) {
                User admin = new User();
                admin.setName("Admin");
                admin.setEmail("admin@bookfair.com");
                admin.setPhone("+94 76 280 0321");
                admin.setRole(UserRole.EXHIBITION_ORGANIZER);
                userRepository.save(admin);
            } else {
                User admin = existingAdmin.get();
                admin.setRole(UserRole.EXHIBITION_ORGANIZER);
                if (admin.getSub() == null) {
                }
                userRepository.save(admin);
            }

            List<String> genreNames = List.of(
                    "Fiction & Literature",
                    "Children & Young Adult",
                    "Educational & Academic",
                    "Science & Technology",
                    "Business & Management",
                    "Arts, Design & Photography",
                    "History & Culture",
                    "Health, Wellness & Lifestyle",
                    "Religion, Philosophy & Spirituality",
                    "Others");
            for (String name : genreNames) {
                if (genreRepository.findAllByOrderByNameAsc().stream().noneMatch(g -> g.getName().equals(name))) {
                    Genre g = new Genre();
                    g.setName(name);
                    genreRepository.save(g);
                }
            }

            if (contentService.get("home.title").isBlank()) {
                contentService.set("home.title", "Book Fair Management System");
                contentService.set("home.description", "Reserve your stall at the best book fairs. Get started today.");
                contentService.set("home.videoUrl", "https://youtu.be/lwdNcMkOiTM?si=QqPJEdjGJF4U-Jub");
            }

            if (contentService.get("contact.email").isBlank()) {
                contentService.set("contact.email", "contact@bookfair.com");
            }
            if (contentService.get("contact.phone").isBlank()) {
                contentService.set("contact.phone", "+94 76 880 0321");
            }
            if (contentService.get("contact.address").isBlank()) {
                contentService.set("contact.address", "SWRD Bandaranaike National Memorial Foundation Bauddhaloka Mawatha, Colombo 07");
            }

            if (eventRepository.count() == 0) {
                // Default event creation removed as per requirement
            }
        };
    }
}