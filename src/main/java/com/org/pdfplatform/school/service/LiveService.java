package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.LiveMessage;
import com.org.pdfplatform.school.model.LiveSession;
import com.org.pdfplatform.school.repo.LiveMessageRepository;
import com.org.pdfplatform.school.repo.LiveSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Live learning: the teacher opens a PDF for a class and moves the page; students follow it and can ask questions. */
@Service
public class LiveService {

    private final LiveSessionRepository sessions;
    private final LiveMessageRepository messages;

    public LiveService(LiveSessionRepository sessions, LiveMessageRepository messages) {
        this.sessions = sessions;
        this.messages = messages;
    }

    @Transactional
    public LiveSession start(Integer classLevel, Long materialId, String title) {
        if (classLevel == null || classLevel < 1 || classLevel > 10) {
            throw new IllegalArgumentException("Class must be 1 to 10.");
        }
        if (materialId == null) {
            throw new IllegalArgumentException("Choose the PDF to teach from.");
        }
        for (LiveSession s : sessions.findByActiveTrue()) {
            if (s.getClassLevel().equals(classLevel)) {
                s.setActive(false);
                sessions.save(s);
            }
        }
        LiveSession s = new LiveSession();
        s.setClassLevel(classLevel);
        s.setMaterialId(materialId);
        s.setTitle(title == null || title.isBlank() ? "Live class " + classLevel : title.trim());
        s.setCurrentPage(1);
        s.setActive(true);
        s.setStartedAt(LocalDateTime.now());
        return sessions.save(s);
    }

    @Transactional
    public void stop(Long sessionId) {
        sessions.findById(sessionId).ifPresent(s -> {
            s.setActive(false);
            sessions.save(s);
        });
    }

    @Transactional
    public void setPage(Long sessionId, int page) {
        LiveSession s = sessions.findById(sessionId).orElseThrow(() -> new IllegalArgumentException("Unknown session"));
        s.setCurrentPage(Math.max(1, Math.min(page, 5000)));
        sessions.save(s);
    }

    public Optional<LiveSession> activeFor(Integer classLevel) {
        return sessions.findFirstByClassLevelAndActiveTrueOrderByIdDesc(classLevel);
    }

    public List<LiveSession> allActive() {
        return sessions.findByActiveTrue();
    }

    @Transactional
    public void post(Long sessionId, SchoolUser author, String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return;
        }
        LiveMessage m = new LiveMessage();
        m.setSessionId(sessionId);
        m.setAuthorName(author.getFullName() == null ? author.getUsername() : author.getFullName());
        m.setAuthorRole(author.getRole().name());
        m.setText(t.length() > 600 ? t.substring(0, 600) : t);
        m.setCreatedAt(LocalDateTime.now());
        messages.save(m);
    }

    /** Oldest first, newest 60. */
    public List<LiveMessage> recent(Long sessionId) {
        List<LiveMessage> list = new ArrayList<>(messages.findTop60BySessionIdOrderByIdDesc(sessionId));
        Collections.reverse(list);
        return list;
    }
}
