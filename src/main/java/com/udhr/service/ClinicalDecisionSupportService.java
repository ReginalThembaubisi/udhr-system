package com.udhr.service;

import com.udhr.model.*;
import com.udhr.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ClinicalDecisionSupportService {

    @Autowired
    private LabRecommendationRepository labRecommendationRepository;

    @Autowired
    private DifferentialDiagnosisRepository differentialDiagnosisRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    public void generateRecommendationsForAlert(ClinicalAlert alert) {
        Patient patient = alert.getPatient();
        List<Prescription> prescriptions = prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId());
        List<Diagnosis> diagnoses = diagnosisRepository.findByPatientIdOrderByDiagnosedAtDesc(patient.getId());

        boolean hasTb = false;
        boolean hasMalaria = false;
        boolean hasDiabetes = false;
        boolean hasHypertension = false;

        // Check diagnoses
        for (Diagnosis d : diagnoses) {
            String diag = d.getDiagnosis().toLowerCase();
            if (diag.contains("tuberculosis") || diag.contains("tb")) hasTb = true;
            if (diag.contains("malaria")) hasMalaria = true;
            if (diag.contains("diabetes")) hasDiabetes = true;
            if (diag.contains("hypertension")) hasHypertension = true;
        }

        // Check prescriptions
        for (Prescription rx : prescriptions) {
            if (!rx.getActive()) continue;
            String med = rx.getMedication().toLowerCase();
            if (med.contains("rifampin") || med.contains("isoniazid") || med.contains("pyrazinamide") || med.contains("ethambutol")) hasTb = true;
            if (med.contains("artemether") || med.contains("lumefantrine") || med.contains("chloroquine") || med.contains("quinine")) hasMalaria = true;
            if (med.contains("metformin") || med.contains("gliclazide") || med.contains("insulin")) hasDiabetes = true;
            if (med.contains("amlodipine") || med.contains("enalapril") || med.contains("losartan") || med.contains("hydrochlorothiazide")) hasHypertension = true;
        }

        // 1. Tuberculosis (TB) recommendations
        if (hasTb) {
            LabRecommendation lr = new LabRecommendation();
            lr.setAlert(alert);
            lr.setPatient(patient);
            lr.setTestName("GeneXpert MTB/RIF");
            lr.setReason("Standard South African TB treatment is failing despite >90% patient adherence. High risk of drug resistance. Recommended to run GeneXpert to screen for Rifampicin-resistant Mycobacterium tuberculosis (MDR-TB).");
            lr.setIcdCode("A15");
            labRecommendationRepository.save(lr);

            DifferentialDiagnosis dd = new DifferentialDiagnosis();
            dd.setAlert(alert);
            dd.setPatient(patient);
            dd.setIcdCode("U84.3");
            dd.setConditionName("Multi-Drug Resistant Tuberculosis (MDR-TB)");
            dd.setLikelihood(DifferentialDiagnosis.Likelihood.HIGH);
            dd.setReasoning("Patient is highly compliant with first-line ARVs/TB therapy but remains symptomatic. Suggests failure of standard Rifampicin/Isoniazid regimen due to secondary drug resistance.");
            differentialDiagnosisRepository.save(dd);
        }

        // 2. Malaria recommendations
        if (hasMalaria) {
            LabRecommendation lr = new LabRecommendation();
            lr.setAlert(alert);
            lr.setPatient(patient);
            lr.setTestName("Thick & Thin Blood Smear or PCR");
            lr.setReason("Malaria therapy failing. Flagged for risk zones (Mpumalanga & Limpopo border regions). Blood smear is required to estimate parasite density and check for chloroquine/artemisin resistance.");
            lr.setIcdCode("B50");
            labRecommendationRepository.save(lr);

            DifferentialDiagnosis dd = new DifferentialDiagnosis();
            dd.setAlert(alert);
            dd.setPatient(patient);
            dd.setIcdCode("B54");
            dd.setConditionName("Drug-Resistant Plasmodium Falciparum Malaria");
            dd.setLikelihood(DifferentialDiagnosis.Likelihood.HIGH);
            dd.setReasoning("Persistent febrile episodes despite completion of Coartem/quinine courses. Risk is elevated in border transit corridors.");
            differentialDiagnosisRepository.save(dd);
        }

        // 3. Diabetes recommendations
        if (hasDiabetes) {
            LabRecommendation lr = new LabRecommendation();
            lr.setAlert(alert);
            lr.setPatient(patient);
            lr.setTestName("HbA1c & Fasting Insulin");
            lr.setReason("Diabetes glycemic control failing despite high medication compliance. Recommended to run HbA1c to measure 3-month average glucose control and Fasting Insulin to evaluate actual insulin secretory capacity.");
            lr.setIcdCode("E11");
            labRecommendationRepository.save(lr);

            // Alternate 1: LADA
            DifferentialDiagnosis dd1 = new DifferentialDiagnosis();
            dd1.setAlert(alert);
            dd1.setPatient(patient);
            dd1.setIcdCode("E13");
            dd1.setConditionName("Latent Autoimmune Diabetes in Adults (LADA)");
            dd1.setLikelihood(DifferentialDiagnosis.Likelihood.MODERATE);
            dd1.setReasoning("Patient is diagnosed with Type 2 Diabetes but fails oral hypoglycemics (Metformin) despite high adherence. Possible autoimmune destruction of beta-cells simulating Type 1.5.");
            differentialDiagnosisRepository.save(dd1);

            // Alternate 2: Insulin Resistance Type 2
            DifferentialDiagnosis dd2 = new DifferentialDiagnosis();
            dd2.setAlert(alert);
            dd2.setPatient(patient);
            dd2.setIcdCode("E11.9");
            dd2.setConditionName("Severe Insulin Resistance Syndrome");
            dd2.setLikelihood(DifferentialDiagnosis.Likelihood.HIGH);
            dd2.setReasoning("Glycemic markers remain critical. Elevated tissue receptor resistance requiring dual-therapy or basal insulin introduction.");
            differentialDiagnosisRepository.save(dd2);
        }

        // 4. Hypertension recommendations
        if (hasHypertension) {
            LabRecommendation lr = new LabRecommendation();
            lr.setAlert(alert);
            lr.setPatient(patient);
            lr.setTestName("Renal Doppler Ultrasound / CTA");
            lr.setReason("Resistant hypertension failing control. Renal artery stenosis screening recommended to rule out renovascular cause of drug resistance.");
            lr.setIcdCode("I10");
            labRecommendationRepository.save(lr);

            // Alternate 1: Renovascular Hypertension
            DifferentialDiagnosis dd1 = new DifferentialDiagnosis();
            dd1.setAlert(alert);
            dd1.setPatient(patient);
            dd1.setIcdCode("I15.0");
            dd1.setConditionName("Renovascular Hypertension");
            dd1.setLikelihood(DifferentialDiagnosis.Likelihood.MODERATE);
            dd1.setReasoning("Blood pressure remains elevated despite compliance with anti-hypertensives. Renovascular narrowing causes hyperactivation of renin-angiotensin-aldosterone system.");
            differentialDiagnosisRepository.save(dd1);

            // Alternate 2: Renal Hypertension
            DifferentialDiagnosis dd2 = new DifferentialDiagnosis();
            dd2.setAlert(alert);
            dd2.setPatient(patient);
            dd2.setIcdCode("I15.1");
            dd2.setConditionName("Hypertension Secondary to Renal Disorders");
            dd2.setLikelihood(DifferentialDiagnosis.Likelihood.MODERATE);
            dd2.setReasoning("Potential nephropathic insufficiency causing sodium and water retention resistant to primary vasodilation therapies.");
            differentialDiagnosisRepository.save(dd2);
        }
    }
}
