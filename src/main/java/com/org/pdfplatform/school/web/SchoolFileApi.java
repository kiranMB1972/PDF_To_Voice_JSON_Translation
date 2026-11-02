package com.org.pdfplatform.school.web;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.SchoolException;
import com.org.pdfplatform.school.model.Material;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.service.MaterialService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** Serves a PDF to the people allowed to see it: the admin (all) or a student (own class PDFs + PDFs sent to that student). */
@RestController
@RequestMapping("/school/files")
public class SchoolFileApi {

    private final MaterialService materials;

    public SchoolFileApi(MaterialService materials) {
        this.materials = materials;
    }

    @GetMapping("/material/{id}")
    public ResponseEntity<byte[]> material(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download, HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        Material m = materials.find(id).orElse(null);
        if (m == null || !materials.canAccess(m, me)) {
            throw new SchoolException(ErrorCodeModel.ACCESS_DENIED, 403, "You cannot open this PDF", "Students see only their class PDFs and the PDFs sent to them.");
        }
        ContentDisposition cd = (download ? ContentDisposition.attachment() : ContentDisposition.inline()).filename(m.getFileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff").body(materials.bytes(id));
    }
}
