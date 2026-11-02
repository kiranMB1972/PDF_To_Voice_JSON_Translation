package com.org.pdfplatform.model;

import java.util.List;

/**
 * detectedLanguage: kn, ta, te, ml, mr, hi, ur or en (from the writing system of the text).
 * truncated: the character limit stopped reading early - the remaining pages are marked NOT_READ.
 */
public record DocumentAnalysisModel(String documentId, String fileName, int totalPages, int readablePages,
                                    String detectedLanguage, boolean truncated, int extractedChars,
                                    List<PageModel> pages, List<String> skippedContent) { }
