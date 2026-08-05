package com.udhr.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClinicalAlertScheduler {

    @Autowired
    private TreatmentResponseService treatmentResponseService;

    // Run every day at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyClinicalEvaluation() {
        System.out.println("[ClinicalAlertScheduler] Starting daily treatment response evaluation...");
        treatmentResponseService.evaluateAllPatients();
        System.out.println("[ClinicalAlertScheduler] Daily evaluation complete.");
    }
}
