package com.udhr.dto;

import lombok.Data;

@Data
public class AnnouncementRequest {
    private String title;
    private String message;
    private String photoUrl;
    private Boolean active;
}
