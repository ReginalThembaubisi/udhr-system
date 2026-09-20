package com.udhr.controller;

import com.udhr.dto.ChatbotRequest;
import com.udhr.dto.ChatbotResponse;
import com.udhr.service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody ChatbotRequest request) {
        try {
            String idNumber = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            ChatbotResponse response = chatbotService.respond(idNumber, request.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
