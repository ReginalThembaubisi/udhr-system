package com.udhr.config;

import com.udhr.model.Facility;
import com.udhr.model.Staff;
import com.udhr.repository.FacilityRepository;
import com.udhr.repository.StaffRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (facilityRepository.count() > 0) {
            System.out.println("Database seeding skipped: Facility data already exists.");
            return;
        }

        // Create Facility
        Facility facility = new Facility();
        facility.setName("Rob Ferreira Hospital");
        facility.setType("HOSPITAL");
        facility.setProvince("Mpumalanga");
        facility.setAddress("Nelspruit, Mpumalanga");
        facility = facilityRepository.save(facility);

        // Staff 1: ADMIN001
        Staff admin = new Staff();
        admin.setStaffNumber("ADMIN001");
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setRole("ADMIN");
        admin.setFacility(facility);
        admin.setEmail("admin@udhr.gov.za");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setActive(true);
        staffRepository.save(admin);

        // Staff 2: DOC001
        Staff doctor = new Staff();
        doctor.setStaffNumber("DOC001");
        doctor.setFirstName("Themba");
        doctor.setLastName("Ubisi");
        doctor.setRole("DOCTOR");
        doctor.setFacility(facility);
        doctor.setEmail("themba@udhr.gov.za");
        doctor.setPassword(passwordEncoder.encode("Doctor@123"));
        doctor.setActive(true);
        staffRepository.save(doctor);

        // Staff 3: NUR001
        Staff nurse = new Staff();
        nurse.setStaffNumber("NUR001");
        nurse.setFirstName("Zanele");
        nurse.setLastName("Mokoena");
        nurse.setRole("NURSE");
        nurse.setFacility(facility);
        nurse.setEmail("zanele@udhr.gov.za");
        nurse.setPassword(passwordEncoder.encode("Nurse@123"));
        nurse.setActive(true);
        staffRepository.save(nurse);

        System.out.println("Database seeded successfully");
    }
}
