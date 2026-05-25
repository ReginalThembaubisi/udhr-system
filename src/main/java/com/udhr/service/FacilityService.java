package com.udhr.service;

import com.udhr.dto.FacilityRequest;
import com.udhr.model.Facility;
import com.udhr.repository.FacilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FacilityService {

    @Autowired
    private FacilityRepository facilityRepository;

    public Facility addFacility(FacilityRequest request) {
        Facility facility = new Facility();
        facility.setName(request.getName());
        facility.setType(request.getType());
        facility.setProvince(request.getProvince());
        facility.setAddress(request.getAddress());

        return facilityRepository.save(facility);
    }

    public List<Facility> getAllFacilities() {
        return facilityRepository.findAll();
    }
}
