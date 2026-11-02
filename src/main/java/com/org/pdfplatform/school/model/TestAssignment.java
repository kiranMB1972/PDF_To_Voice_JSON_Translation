package com.org.pdfplatform.school.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sch_test_assignments", uniqueConstraints = @UniqueConstraint(columnNames = {"test_id", "student_id"}))
public class TestAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTestId() { return testId; }
    public void setTestId(Long testId) { this.testId = testId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
}
