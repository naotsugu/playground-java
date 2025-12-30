package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;
import java.io.StringReader;

import static com.mammb.code.toml.impl.TomlTokenizerAssertions.assertNext;

public class TomlTokenizerTextBlockTest {

    @Test void textBlock() {
        var tokenizer = new TomlTokenizer(new StringReader("text = \"\"\"Here are two quotation marks: \"\". Simple enough.\"\"\""));
        assertNext(tokenizer, TomlToken.STRING, "text");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "Here are two quotation marks: \"\". Simple enough.");
    }

    @Test void literalTextBlock() {
        var tokenizer = new TomlTokenizer(new StringReader("regex = '''I [dw]on't need \\d{2} apples'''"));
        assertNext(tokenizer, TomlToken.STRING, "regex");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "I [dw]on't need \\d{2} apples");
    }

}
