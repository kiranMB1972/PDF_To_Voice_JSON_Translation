package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.SchoolSetting;
import com.org.pdfplatform.school.repo.SchoolSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SchoolService {

    private final SchoolSettingRepository repo;

    public SchoolService(SchoolSettingRepository repo) {
        this.repo = repo;
    }

    public String name() {
        return repo.findById(1L).map(SchoolSetting::getSchoolName).orElse("My School");
    }

    @Transactional
    public void rename(String newName) {
        String n = newName == null ? "" : newName.trim();
        if (n.length() < 2 || n.length() > 160) {
            throw new IllegalArgumentException("The school name must have 2 to 160 characters.");
        }
        SchoolSetting s = repo.findById(1L).orElseGet(SchoolSetting::new);
        s.setId(1L);
        s.setSchoolName(n);
        s.setUpdatedAt(LocalDateTime.now());
        repo.save(s);
    }
}
