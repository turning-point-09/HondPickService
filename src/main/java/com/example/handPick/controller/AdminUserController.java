package com.example.handPick.controller;

import com.example.handPick.dto.UserDto;
import com.example.handPick.dto.UserStatusDto;
import com.example.handPick.dto.UserStatsDto;
import com.example.handPick.model.User;
import com.example.handPick.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<UserDto> users = userService.getAllUsers(pageRequest);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/active")
    public ResponseEntity<Page<UserDto>> getActiveUsers(
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<UserDto> users = userService.getActiveUsers(pageRequest);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/inactive")
    public ResponseEntity<Page<UserDto>> getInactiveUsers(
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Sort sort = sortOrder.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<UserDto> users = userService.getInactiveUsers(pageRequest);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/stats")
    public ResponseEntity<UserStatsDto> getUserStats() {
        UserStatsDto stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    @PutMapping("/{userId}/activate")
    public ResponseEntity<UserStatusDto> activateUser(@PathVariable Long userId) {
        User user = userService.activateUser(userId);
        UserStatusDto response = new UserStatusDto(userId, true, "User activated successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/deactivate")
    public ResponseEntity<UserStatusDto> deactivateUser(@PathVariable Long userId) {
        User user = userService.deactivateUser(userId);
        UserStatusDto response = new UserStatusDto(userId, false, "User deactivated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<UserStatusDto> getUserStatus(@PathVariable Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        UserStatusDto response = new UserStatusDto(userId, user.getActive(), 
            user.getActive() ? "User is active" : "User is inactive");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<UserStatusDto> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusDto statusDto) {
        User user;
        String message;
        
        if (statusDto.getActive()) {
            user = userService.activateUser(userId);
            message = "User activated successfully";
        } else {
            user = userService.deactivateUser(userId);
            message = "User deactivated successfully";
        }
        
        UserStatusDto response = new UserStatusDto(userId, user.getActive(), message);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserStatusDto> patchUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusDto statusDto) {
        User user;
        String message;
        
        if (statusDto.getActive()) {
            user = userService.activateUser(userId);
            message = "User activated successfully";
        } else {
            user = userService.deactivateUser(userId);
            message = "User deactivated successfully";
        }
        
        UserStatusDto response = new UserStatusDto(userId, user.getActive(), message);
        return ResponseEntity.ok(response);
    }
} 