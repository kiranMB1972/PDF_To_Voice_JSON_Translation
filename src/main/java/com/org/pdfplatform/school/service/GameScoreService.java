package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.GameScore;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.repo.GameScoreRepository;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GameScoreService {

    public static final Set<String> GAMES = Set.of("math-sprint", "memory-match", "word-unscramble", "quiz-blitz");

    private final GameScoreRepository scores;
    private final SchoolUserRepository users;

    public GameScoreService(GameScoreRepository scores, SchoolUserRepository users) {
        this.scores = scores;
        this.users = users;
    }

    @Transactional
    public int save(Long studentId, String game, int score) {
        if (!GAMES.contains(game)) {
            throw new IllegalArgumentException("Unknown game '" + game + "'.");
        }
        if (score < 0 || score > 100000) {
            throw new IllegalArgumentException("That score is not possible.");
        }
        GameScore g = new GameScore();
        g.setStudentId(studentId);
        g.setGame(game);
        g.setScore(score);
        g.setPlayedAt(LocalDateTime.now());
        scores.save(g);
        return scores.findFirstByGameAndStudentIdOrderByScoreDesc(game, studentId).map(GameScore::getScore).orElse(score);
    }

    public List<Map<String, Object>> top(String game) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (GameScore g : scores.findTop10ByGameOrderByScoreDescPlayedAtAsc(game)) {
            SchoolUser u = users.findById(g.getStudentId()).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", u == null ? "?" : (u.getFullName() == null ? u.getUsername() : u.getFullName()));
            m.put("classLevel", u == null ? null : u.getClassLevel());
            m.put("score", g.getScore());
            out.add(m);
        }
        return out;
    }
}
