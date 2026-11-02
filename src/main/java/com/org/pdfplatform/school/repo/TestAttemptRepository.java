package com.org.pdfplatform.school.repo;

import com.org.pdfplatform.school.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    Optional<TestAttempt> findByTestIdAndStudentId(Long testId, Long studentId);
    List<TestAttempt> findByTestId(Long testId);
    List<TestAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);
    @Modifying
    @Query("delete from TestAttempt a where a.testId = :tid")
    void deleteByTestId(@Param("tid") Long testId);
    @Modifying
    @Query("delete from TestAttempt a where a.studentId = :sid")
    void deleteByStudentId(@Param("sid") Long studentId);
}
