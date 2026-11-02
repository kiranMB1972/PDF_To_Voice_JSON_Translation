package com.org.pdfplatform.school.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sch_live_sessions")
public class LiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_level", nullable = false)
    private Integer classLevel;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "title", length = 160)
    private String title;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage;

    @Column(name = "active")
    private boolean active;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getClassLevel() { return classLevel; }
    public void setClassLevel(Integer classLevel) { this.classLevel = classLevel; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getCurrentPage() { return currentPage; }
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
