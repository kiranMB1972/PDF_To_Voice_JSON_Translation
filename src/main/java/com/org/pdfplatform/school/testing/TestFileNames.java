package com.org.pdfplatform.school.testing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** File name rule: Test&lt;DD-MM-YYYY&gt;.properties (a second test on the same day gets _2, _3 ...). */
public final class TestFileNames {

    public static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private TestFileNames() { }

    public static String forDate(LocalDate date) {
        return "Test" + DMY.format(date) + ".properties";
    }

    public static String forDate(LocalDate date, int sequence) {
        return sequence <= 1 ? forDate(date) : "Test" + DMY.format(date) + "_" + sequence + ".properties";
    }
}
