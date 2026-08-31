package com.bookfair.Stall_Reservation.controller.admin;

import com.bookfair.Stall_Reservation.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/profile")
public class AdminProfileController {

    private final AdminService adminService;

    public AdminProfileController(AdminService adminService) {
        this.adminService = adminService;
    }

    private Long currentUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null)
            return null;
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<?> get(Authentication auth) {
        Long userId = currentUserId(auth);
        if (userId == null)
            return ResponseEntity.status(401).build();
        Map<String, Object> profile = adminService.getProfile(userId);
        if (profile == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = currentUserId(auth);
        if (userId == null)
            return ResponseEntity.status(401).build();
        try {
            adminService.updateProfile(userId, body);
            return ResponseEntity.ok(Map.of("message", "Profile updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @GetMapping("/admins")
    public ResponseEntity<List<Map<String, Object>>> listAdmins() {
        return ResponseEntity.ok(adminService.listAdmins());
    }


    @DeleteMapping("/admins/{id}")
    public ResponseEntity<?> removeAdmin(@PathVariable Long id, Authentication auth) {
        Long currentId = currentUserId(auth);
        if (currentId != null && currentId.equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot remove yourself"));
        }
        try {
            adminService.removeAdmin(id);
            return ResponseEntity.ok(Map.of("message", "Admin removed"));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
