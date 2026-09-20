package com.udhr.service;

import com.udhr.dto.AnnouncementRequest;
import com.udhr.model.Announcement;
import com.udhr.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AnnouncementService {

    @Autowired
    private AnnouncementRepository announcementRepository;

    public List<Announcement> getPublicAnnouncements() {
        return announcementRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByCreatedAtDesc();
    }

    public Announcement createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        announcement.setPhotoUrl(request.getPhotoUrl());
        announcement.setActive(request.getActive() == null || request.getActive());
        return announcementRepository.save(announcement);
    }

    public Announcement updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getMessage() != null) announcement.setMessage(request.getMessage());
        if (request.getPhotoUrl() != null) announcement.setPhotoUrl(request.getPhotoUrl());
        if (request.getActive() != null) announcement.setActive(request.getActive());
        return announcementRepository.save(announcement);
    }

    public void deleteAnnouncement(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new RuntimeException("Announcement not found");
        }
        announcementRepository.deleteById(id);
    }
}
