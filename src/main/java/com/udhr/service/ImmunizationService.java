package com.udhr.service;

import com.udhr.dto.ImmunizationAdministerRequest;
import com.udhr.dto.ImmunizationRequest;
import com.udhr.model.Immunization;
import com.udhr.model.Patient;
import com.udhr.model.Staff;
import com.udhr.repository.ImmunizationRepository;
import com.udhr.repository.PatientRepository;
import com.udhr.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * South African Expanded Programme on Immunisation (EPI) schedule.
 * Source: National Department of Health EPI-SA schedule. Simplified for
 * a student project: core doses only, not every catch-up/booster variant.
 */
@Service
public class ImmunizationService {

    @Autowired
    private ImmunizationRepository immunizationRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private StaffRepository staffRepository;

    private record ScheduleEntry(String vaccineName, int doseNumber, int weeksAfterBirth) {}

    private static final List<ScheduleEntry> EPI_SCHEDULE = List.of(
            new ScheduleEntry("BCG", 1, 0),
            new ScheduleEntry("OPV (Birth dose)", 0, 0),
            new ScheduleEntry("Rotavirus", 1, 6),
            new ScheduleEntry("OPV", 1, 6),
            new ScheduleEntry("PCV", 1, 6),
            new ScheduleEntry("Hexavalent (DTaP-IPV-Hib-HepB)", 1, 6),
            new ScheduleEntry("Hexavalent (DTaP-IPV-Hib-HepB)", 2, 10),
            new ScheduleEntry("Rotavirus", 2, 14),
            new ScheduleEntry("PCV", 2, 14),
            new ScheduleEntry("Hexavalent (DTaP-IPV-Hib-HepB)", 3, 14),
            new ScheduleEntry("Measles", 1, 26),
            new ScheduleEntry("PCV", 3, 39),
            new ScheduleEntry("Measles", 2, 52),
            new ScheduleEntry("Hexavalent Booster (DTaP-IPV-Hib)", 4, 78)
    );

    /**
     * Generates the full EPI schedule for a patient, dated from their date of
     * birth. Idempotent: if the patient already has immunization records,
     * returns those instead of duplicating them.
     */
    @Transactional
    public List<Immunization> generateEpiSchedule(Patient patient) {
        List<Immunization> existing = immunizationRepository.findByPatientIdOrderByScheduledDateAsc(patient.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        List<Immunization> created = new ArrayList<>();
        for (ScheduleEntry entry : EPI_SCHEDULE) {
            Immunization immunization = new Immunization();
            immunization.setPatient(patient);
            immunization.setVaccineName(entry.vaccineName());
            immunization.setDoseNumber(entry.doseNumber());
            immunization.setScheduledDate(patient.getDateOfBirth().plusWeeks(entry.weeksAfterBirth()));
            immunization.setStatus("DUE");
            created.add(immunizationRepository.save(immunization));
        }
        return created;
    }

    public List<Immunization> getImmunizationsByPatient(Long patientId) {
        return immunizationRepository.findByPatientIdOrderByScheduledDateAsc(patientId);
    }

    @Transactional
    public Immunization administerDose(Long immunizationId, ImmunizationAdministerRequest request, String staffNumber) {
        Immunization immunization = immunizationRepository.findById(immunizationId)
                .orElseThrow(() -> new RuntimeException("Immunization record not found"));

        immunization.setStatus("GIVEN");
        immunization.setAdministeredDate(
                (request.getAdministeredDate() != null && !request.getAdministeredDate().isBlank())
                        ? LocalDate.parse(request.getAdministeredDate())
                        : LocalDate.now()
        );
        immunization.setNotes(request.getNotes());

        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        if (staff != null) {
            immunization.setAdministeredBy(staff);
            if (immunization.getFacility() == null) {
                immunization.setFacility(staff.getFacility());
            }
        }

        return immunizationRepository.save(immunization);
    }

    @Transactional
    public Immunization markMissed(Long immunizationId) {
        Immunization immunization = immunizationRepository.findById(immunizationId)
                .orElseThrow(() -> new RuntimeException("Immunization record not found"));
        immunization.setStatus("MISSED");
        return immunizationRepository.save(immunization);
    }

    /**
     * Adds a manual/catch-up immunization record outside the standard EPI
     * schedule (e.g. a travel vaccine, or a dose given at another facility
     * before this patient's file existed in UDHR).
     */
    @Transactional
    public Immunization addRecord(ImmunizationRequest request, String staffNumber) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        Immunization immunization = new Immunization();
        immunization.setPatient(patient);
        immunization.setVaccineName(request.getVaccineName());
        immunization.setDoseNumber(request.getDoseNumber());
        immunization.setScheduledDate(
                (request.getScheduledDate() != null && !request.getScheduledDate().isBlank())
                        ? LocalDate.parse(request.getScheduledDate())
                        : LocalDate.now()
        );

        boolean alreadyGiven = request.getAdministeredDate() != null && !request.getAdministeredDate().isBlank();
        if (alreadyGiven) {
            immunization.setAdministeredDate(LocalDate.parse(request.getAdministeredDate()));
            immunization.setStatus("GIVEN");
        } else {
            immunization.setStatus("DUE");
        }
        immunization.setNotes(request.getNotes());

        Staff staff = staffRepository.findByStaffNumber(staffNumber).orElse(null);
        if (staff != null) {
            immunization.setFacility(staff.getFacility());
            if (alreadyGiven) {
                immunization.setAdministeredBy(staff);
            }
        }

        return immunizationRepository.save(immunization);
    }
}
