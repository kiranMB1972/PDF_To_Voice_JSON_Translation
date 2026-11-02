package com.org.pdfplatform.school.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sch_tests")
public class TestDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "class_level", nullable = false)
    private Integer classLevel;

    @Column(name = "subject", nullable = false, length = 60)
    private String subject;

    @Column(name = "test_date", nullable = false)
    private LocalDate testDate;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "material_id")
    private Long materialId;

    @Column(name = "active")
    private boolean active;

    @Column(name = "properties_file", nullable = false, length = 80)
    private String propertiesFile;

    @Lob
    @Column(name = "properties_text", nullable = false, columnDefinition = "LONGTEXT")
    private String propertiesText;

    @Column(name = "created_by", length = 60)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getClassLevel() { return classLevel; }
    public void setClassLevel(Integer classLevel) { this.classLevel = classLevel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDate getTestDate() { return testDate; }
    public void setTestDate(LocalDate testDate) { this.testDate = testDate; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getPropertiesFile() { return propertiesFile; }
    public void setPropertiesFile(String propertiesFile) { this.propertiesFile = propertiesFile; }
    public String getPropertiesText() { return propertiesText; }
    public void setPropertiesText(String propertiesText) { this.propertiesText = propertiesText; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
