package com.org.pdfplatform.school.repo;

import com.org.pdfplatform.school.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameScoreRepository extends JpaRepository<GameScore, Long> {
    List<GameScore> findTop10ByGameOrderByScoreDescPlayedAtAsc(String game);
    Optional<GameScore> findFirstByGameAndStudentIdOrderByScoreDesc(String game, Long studentId);
    @Modifying
    @Query("delete from GameScore g where g.studentId = :sid")
    void deleteByStudentId(@Param("sid") Long studentId);
}
