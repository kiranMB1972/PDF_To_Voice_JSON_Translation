package com.org.pdfplatform.model;

import java.util.List;

public record PageModel(int sequence, boolean readable, List<TextBlockModel> blocks) { }
