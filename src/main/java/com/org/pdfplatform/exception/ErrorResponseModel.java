package com.org.pdfplatform.exception;

/**
 * JSON error body. The page turns it into a coloured, animated notice (icon + reason + steps + idea), never raw Java text.
 * "guide" is filled automatically from ErrorGuide, so every error of the API carries its own explanation.
 */
public record ErrorResponseModel(ErrorCodeModel code, String error, String userGuidance, String workaround,
                                 String fyiLink, ErrorGuide.Guide guide) {

    public ErrorResponseModel(ErrorCodeModel code, String error, String userGuidance, String workaround, String fyiLink) {
        this(code, error, userGuidance, workaround, fyiLink, ErrorGuide.forCode(code));
    }
}
