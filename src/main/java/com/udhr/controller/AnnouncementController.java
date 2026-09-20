package com.udhr.controller;

import com.udhr.dto.AnnouncementRequest;
import com.udhr.model.Announcement;
import com.udhr.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    // Unauthenticated — the login screen fetches this before anyone signs in.
    @GetMapping("/public")
    public ResponseEntity<?> getPublicAnnouncements() {
        List<Announcement> announcements = announcementService.getPublicAnnouncements();
        return ResponseEntity.ok(announcements);
    }

    @GetMapping
    public ResponseEntity<?> getAllAnnouncements() {
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        return ResponseEntity.ok(announcements);
    }

    @PostMapping
    public ResponseEntity<?> createAnnouncement(@RequestBody AnnouncementRequest request) {
        try {
            Announcement announcement = announcementService.createAnnouncement(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(announcement);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnnouncement(@PathVariable Long id, @RequestBody AnnouncementRequest request) {
        try {
            Announcement announcement = announcementService.updateAnnouncement(id, request);
            return ResponseEntity.ok(announcement);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        try {
            announcementService.deleteAnnouncement(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
