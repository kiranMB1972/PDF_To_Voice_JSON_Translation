package com.org.pdfplatform.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** The catalogue of visual errors (colours, animated scene, reason, steps, idea) for the browser notices. */
@RestController
public class ErrorGuideController {

    @GetMapping("/api/error-guides")
    public Map<String, ErrorGuide.Guide> guides() {
        return ErrorGuide.all();
    }
}
