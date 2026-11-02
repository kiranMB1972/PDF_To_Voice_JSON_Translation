package com.org.pdfplatform.school.repo;

import com.org.pdfplatform.school.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TestDefRepository extends JpaRepository<TestDef, Long> {
    List<TestDef> findAllByOrderByTestDateDescIdDesc();
    long countByTestDate(LocalDate date);
    List<TestDef> findByPropertiesFileStartingWith(String prefix);
}
