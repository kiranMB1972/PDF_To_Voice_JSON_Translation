package com.org.pdfplatform.model;

/**
 * type: TEXT (readable), IMAGE_ONLY (picture/scan page), GARBLED_FONT (text layer exists but the font cannot be
 * decoded, e.g. legacy Nudi/Baraha Kannada fonts), NOT_READ (beyond the character limit).
 * Only TEXT blocks are ever spoken or translated.
 */
public record TextBlockModel(String type, String content) {
    public static TextBlockModel text(String content) { return new TextBlockModel("TEXT", content); }
    public static TextBlockModel imageOnly() { return new TextBlockModel("IMAGE_ONLY", ""); }
    public static TextBlockModel garbled() { return new TextBlockModel("GARBLED_FONT", ""); }
    public static TextBlockModel notRead() { return new TextBlockModel("NOT_READ", ""); }
}
