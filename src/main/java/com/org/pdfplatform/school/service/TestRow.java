package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.TestAttempt;
import com.org.pdfplatform.school.model.TestDef;

/** One line of the student's test list. status: OPEN, IN_PROGRESS, DONE, UPCOMING or MISSED. */
public class TestRow {

    private final TestDef test;
    private final TestAttempt attempt;
    private final String status;

    public TestRow(TestDef test, TestAttempt attempt, String status) {
        this.test = test;
        this.attempt = attempt;
        this.status = status;
    }

    public TestDef getTest() { return test; }
    public TestAttempt getAttempt() { return attempt; }
    public String getStatus() { return status; }
}
