package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.mammb.code.toml.impl.TomlTokenizerAssertions.assertNext;

public class TomlTokenizerDateTimeTest {

    @Test
    void localTime() {
        var tokenizer = new TomlTokenizer(new StringReader("lt1 = 07:32:00"));
        assertNext(tokenizer, TomlToken.STRING, "lt1");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.TIME, LocalTime.parse("07:32:00"));
    }

    @Test
    void localDate() {
        var tokenizer = new TomlTokenizer(new StringReader("ld1 = 1979-05-27"));
        assertNext(tokenizer, TomlToken.STRING, "ld1");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.LOCALDATE, LocalDate.parse("1979-05-27"));
    }

}
