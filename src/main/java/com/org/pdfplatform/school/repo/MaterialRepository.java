package com.org.pdfplatform.school.repo;

import com.org.pdfplatform.school.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOrderByClassLevelAscSubjectAscTitleAsc();
    @Query("select m from Material m where m.classLevel = :cl and (m.targetStudentId is null or m.targetStudentId = :sid) order by m.subject, m.title")
    List<Material> findVisibleToStudent(@Param("cl") Integer classLevel, @Param("sid") Long studentId);
    List<Material> findByTargetStudentId(Long studentId);
}
