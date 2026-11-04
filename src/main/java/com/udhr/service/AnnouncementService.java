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
    private AuditService auditService;

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
        Announcement saved = announcementRepository.save(announcement);
        auditService.recordEvent(auditService.currentStaff(), null, "CREATE_ANNOUNCEMENT",
                "created public announcement #" + saved.getId() + " titled '" + saved.getTitle() + "'"
                        + (Boolean.TRUE.equals(saved.getActive()) ? " and published it" : " as a draft"),
                "ANNOUNCEMENT", saved.getId());
        return saved;
    }

    public Announcement updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getMessage() != null) announcement.setMessage(request.getMessage());
        if (request.getPhotoUrl() != null) announcement.setPhotoUrl(request.getPhotoUrl());
        if (request.getActive() != null) announcement.setActive(request.getActive());
        Announcement saved = announcementRepository.save(announcement);
        auditService.recordEvent(auditService.currentStaff(), null, "UPDATE_ANNOUNCEMENT",
                "updated public announcement #" + saved.getId() + " titled '" + saved.getTitle()
                        + "' (" + (Boolean.TRUE.equals(saved.getActive()) ? "published" : "unpublished") + ")",
                "ANNOUNCEMENT", saved.getId());
        return saved;
    }

    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
        String title = announcement.getTitle();
        announcementRepository.deleteById(id);
        auditService.recordEvent(auditService.currentStaff(), null, "DELETE_ANNOUNCEMENT",
                "deleted public announcement #" + id + " titled '" + title + "'", "ANNOUNCEMENT", id);
    }
}
