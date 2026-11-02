package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.Material;
import com.org.pdfplatform.school.model.MaterialData;
import com.org.pdfplatform.school.model.Role;
import com.org.pdfplatform.school.repo.MaterialDataRepository;
import com.org.pdfplatform.school.repo.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MaterialService {

    private final MaterialRepository materials;
    private final MaterialDataRepository dataRepo;

    public MaterialService(MaterialRepository materials, MaterialDataRepository dataRepo) {
        this.materials = materials;
        this.dataRepo = dataRepo;
    }

    @Transactional
    public Material store(MultipartFile file, Integer classLevel, String subject, String title, Long targetStudentId, String uploader)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a PDF file to upload.");
        }
        if (classLevel == null || classLevel < 1 || classLevel > 10) {
            throw new IllegalArgumentException("Class must be 1 to 10.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject is required.");
        }
        byte[] bytes = file.getBytes();
        if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F') {
            throw new IllegalArgumentException("Only PDF files can be uploaded.");
        }
        String original = file.getOriginalFilename() == null ? "file.pdf" : file.getOriginalFilename();
        Material m = new Material();
        m.setClassLevel(classLevel);
        m.setSubject(subject.trim());
        m.setTitle(title == null || title.isBlank() ? original : title.trim());
        m.setFileName(original.length() > 200 ? original.substring(original.length() - 200) : original);
        m.setContentType("application/pdf");
        m.setSizeBytes((long) bytes.length);
        m.setTargetStudentId(targetStudentId);
        m.setUploadedBy(uploader);
        m.setUploadedAt(LocalDateTime.now());
        Material saved = materials.save(m);
        MaterialData d = new MaterialData();
        d.setId(saved.getId());
        d.setData(bytes);
        dataRepo.save(d);
        return saved;
    }

    public Optional<Material> find(Long id) {
        return materials.findById(id);
    }

    public byte[] bytes(Long id) {
        return dataRepo.findById(id).map(MaterialData::getData).orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    public List<Material> all() {
        return materials.findAllByOrderByClassLevelAscSubjectAscTitleAsc();
    }

    public List<Material> visibleTo(SchoolUser student) {
        return materials.findVisibleToStudent(student.getClassLevel(), student.getId());
    }

    /** Admin sees everything; a student sees their class PDFs and the PDFs sent only to them. */
    public boolean canAccess(Material m, SchoolUser u) {
        if (u.getRole() == Role.ADMIN) {
            return true;
        }
        return u.getClassLevel() != null && u.getClassLevel().equals(m.getClassLevel())
                && (m.getTargetStudentId() == null || m.getTargetStudentId().equals(u.getId()));
    }

    @Transactional
    public void delete(Long id) {
        dataRepo.deleteById(id);
        materials.deleteById(id);
    }
}
