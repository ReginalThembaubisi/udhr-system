package com.udhr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private String reply;
    private String urgencyLevel; // RED, YELLOW, GREEN, or null when no triage was performed
    private List<String> matchedSymptoms;
}
