package com.sh7411usa.jrelay.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class CsvUtilTest {

    @Test
    public void parseLine_handlesPlainFields() {
        List<String> fields = CsvUtil.parseLine("+12345678910,John Doe");
        assertEquals(2, fields.size());
        assertEquals("+12345678910", fields.get(0));
        assertEquals("John Doe", fields.get(1));
    }

    @Test
    public void parseLine_handlesQuotedFieldWithComma() {
        List<String> fields = CsvUtil.parseLine("+12345678910,\"Doe, John\"");
        assertEquals(2, fields.size());
        assertEquals("Doe, John", fields.get(1));
    }

    @Test
    public void escapeField_quotesOnlyWhenNeeded() {
        assertEquals("John Doe", CsvUtil.escapeField("John Doe"));
        assertEquals("\"Doe, John\"", CsvUtil.escapeField("Doe, John"));
        assertEquals("\"Say \"\"hi\"\"\"", CsvUtil.escapeField("Say \"hi\""));
    }

    @Test
    public void roundTrip_escapeThenParseRecoversOriginal() {
        String original = "Doe, \"Johnny\"";
        String escaped = CsvUtil.escapeField(original);
        List<String> fields = CsvUtil.parseLine(escaped);
        assertEquals(1, fields.size());
        assertEquals(original, fields.get(0));
    }
}
