package com.org.pdfplatform.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorMapperAndPagesTest {

    private static MockHttpServletRequest browser(String path) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", path);
        r.setRequestURI(path);
        r.addHeader("Accept", "text/html,application/xhtml+xml,*/*;q=0.8");
        return r;
    }

    @Test
    void exceptionsMapToStatusAndVisualCode() {
        assertEquals(404, ErrorMapper.map(new NoResourceFoundException(HttpMethod.GET, "x"), "/x").status());
        assertEquals(ErrorCodeModel.NOT_FOUND, ErrorMapper.map(new NoResourceFoundException(HttpMethod.GET, "x"), "/x").code());
        // assertEquals(405, ErrorMapper.map(new HttpRequestMethodNotSupportedException("GET", new String[]{"POST"}), "/api/v1/translate").status());
//claude fix:
        assertEquals(405, ErrorMapper.map(new HttpRequestMethodNotSupportedException("GET", java.util.List.of("POST")), "/api/v1/translate").status());

        assertEquals(ErrorCodeModel.FILE_TOO_LARGE, ErrorMapper.map(new MaxUploadSizeExceededException(1L), "/x").code());
        assertEquals(500, ErrorMapper.map(new IllegalStateException("boom"), "/api/x").status());
        assertEquals(400, ErrorMapper.map(new IllegalArgumentException("Class must be 1 to 10."), "/school/api/admin/students").status());
        assertEquals(ErrorCodeModel.VALIDATION, ErrorMapper.map(new IllegalArgumentException("bad"), "/school/api/admin/students").code());
        assertEquals(500, ErrorMapper.map(new IllegalArgumentException("bad"), "/api/other").status());
        SchoolException se = new SchoolException(ErrorCodeModel.LOGIN_FAILED, 401, "wrong", "retry");
        assertEquals(401, ErrorMapper.map(se, "/school/api/auth/login").status());
        assertEquals("retry", ErrorMapper.map(se, "/x").guidance());
    }

    @Test
    void aBrowserGetsTheVisualPageAndTheStatus() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        ModelAndView mv = new VisualErrorResolver().resolveException(browser("/nothing-here"), res, null, new NoResourceFoundException(HttpMethod.GET, "nothing-here"));
        assertNotNull(mv);
        assertEquals(404, res.getStatus());
        assertTrue(res.getContentType().startsWith("text/html"));
        String body = res.getContentAsString();
        assertTrue(body.contains("That page does not exist"));
        assertTrue(body.contains("/nothing-here"));
        assertTrue(body.contains("ERR-"));
    }

    @Test
    void thePagesOwnFetchCallsAreLeftToTheJsonHandler() {
        MockHttpServletRequest api = new MockHttpServletRequest("POST", "/api/v1/translate");
        api.addHeader("Accept", "application/json");
        assertNull(new VisualErrorResolver().resolveException(api, new MockHttpServletResponse(), null, new IllegalStateException("x")));
        MockHttpServletRequest cors = browser("/x");
        cors.addHeader("Sec-Fetch-Mode", "cors");
        assertTrue(!ErrorPageSupport.wantsHtml(cors));
        assertTrue(ErrorPageSupport.wantsHtml(browser("/x")));
    }

    @Test
    void theErrorControllerAnswersBrowsersWithHtmlAndOthersWithJsonThatCarriesTheGuide() {
        MockHttpServletRequest html = browser("/error");
        html.setAttribute("jakarta.servlet.error.status_code", 404);
        html.setAttribute("jakarta.servlet.error.request_uri", "/missing");
        ResponseEntity<?> page = new VisualErrorController().error(html);
        assertEquals(404, page.getStatusCode().value());
        assertTrue(String.valueOf(page.getBody()).contains("<!DOCTYPE html>"));

        MockHttpServletRequest json = new MockHttpServletRequest("GET", "/error");
        json.addHeader("Accept", "application/json");
        json.setAttribute("jakarta.servlet.error.status_code", 405);
        ResponseEntity<?> body = new VisualErrorController().error(json);
        assertEquals(405, body.getStatusCode().value());
        ErrorResponseModel m = (ErrorResponseModel) body.getBody();
        assertEquals(ErrorCodeModel.METHOD_NOT_ALLOWED, m.code());
        assertEquals("METHOD_NOT_ALLOWED", m.guide().code());
    }

    @Test
    void everyJsonErrorCarriesItsGuideAutomatically() {
        ErrorResponseModel m = new ErrorResponseModel(ErrorCodeModel.INVALID_PDF, "bad", "g", "w", null);
        assertEquals("INVALID_PDF", m.guide().code());
        assertTrue(!m.guide().steps().isEmpty());
        assertEquals(ErrorGuide.all().size(), new ErrorGuideController().guides().size());
    }
}
