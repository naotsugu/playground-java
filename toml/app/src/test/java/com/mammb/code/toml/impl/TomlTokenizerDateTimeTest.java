package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
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

}
