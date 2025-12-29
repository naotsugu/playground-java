package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;
import java.io.StringReader;

import static com.mammb.code.toml.impl.TomlTokenizerAssertions.assertNext;

public class TomlTokenizerTextBlockTest {

    @Test void literalTextBlock() {
        var tokenizer = new TomlTokenizer(new StringReader("regex = '''I [dw]on't need \\d{2} apples'''"));
        assertNext(tokenizer, TomlToken.STRING, "regex");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "I [dw]on't need \\d{2} apples");
    }

}
