package com.udhr.repository;

import com.udhr.model.DemoEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemoEmailRepository extends JpaRepository<DemoEmail, Long> {
    List<DemoEmail> findTop20ByOrderBySentAtDesc();
}
