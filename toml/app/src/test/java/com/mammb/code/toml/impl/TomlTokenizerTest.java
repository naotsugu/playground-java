package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static com.mammb.code.toml.impl.TomlTokenizerAssertions.assertNext;
import static org.junit.jupiter.api.Assertions.*;

class TomlTokenizerTest {

    @Test void keyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("key = \"value\""));
        assertNext(tokenizer, TomlToken.STRING, "key");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "value");
    }

    @Test void bareKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=\"b\""));
        assertNext(tokenizer, TomlToken.STRING, "a");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "b");
    }

    @Test void basicKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("\"a\"=\"b\""));
        assertNext(tokenizer, TomlToken.STRING, "a");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "b");
    }

    @Test void literalKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("'a'=\"b\""));
        assertNext(tokenizer, TomlToken.STRING, "a");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "b");
    }

    @Test void infValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=inf"));
        assertNext(tokenizer, TomlToken.STRING, "a");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.INF);
    }

    @Test void negativeInfValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=-inf"));
        assertNext(tokenizer, TomlToken.STRING, "a");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.N_INF);
    }

    @Test void nanValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=nan"));
        assertNext(tokenizer, TomlToken.STRING, "a");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.NAN);
    }

    @Test void intValue() {
        var tokenizer = new TomlTokenizer(new StringReader("int=123"));
        assertNext(tokenizer, TomlToken.STRING, "int");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.INTEGER, 123);
    }

    @Test void intUValue() {
        var tokenizer = new TomlTokenizer(new StringReader("int=1_234"));
        assertNext(tokenizer, TomlToken.STRING, "int");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.INTEGER, 1234);
    }

    @Test void numKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("1234 = \"value\""));
        assertNext(tokenizer, TomlToken.STRING, "1234");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "value");
    }

    @Test void quotedValue() {
        var tokenizer = new TomlTokenizer(new StringReader("'quoted \"value\"' = \"value\""));
        assertNext(tokenizer, TomlToken.STRING, "quoted \"value\"");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "value");
    }

    @Test void dottedKey() {
        var tokenizer = new TomlTokenizer(new StringReader("physical.color = \"orange\""));
        assertNext(tokenizer, TomlToken.STRING, "physical");
        assertNext(tokenizer, TomlToken.DOT);
        assertNext(tokenizer, TomlToken.STRING, "color");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "orange");
    }

    @Test void dottedDottedKey() {
        var tokenizer = new TomlTokenizer(new StringReader("fruit.apple.smooth = true"));
        assertNext(tokenizer, TomlToken.STRING, "fruit");
        assertNext(tokenizer, TomlToken.DOT);
        assertNext(tokenizer, TomlToken.STRING, "apple");
        assertNext(tokenizer, TomlToken.DOT);
        assertNext(tokenizer, TomlToken.STRING, "smooth");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.TRUE);
    }

    @Test void trueValue() {
        var tokenizer = new TomlTokenizer(new StringReader("key = true"));
        assertNext(tokenizer, TomlToken.STRING, "key");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.TRUE);
    }

    @Test void falseValue() {
        var tokenizer = new TomlTokenizer(new StringReader("key = false"));
        assertNext(tokenizer, TomlToken.STRING, "key");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.FALSE);
    }

    @Test void intArray() {
        var tokenizer = new TomlTokenizer(new StringReader("integers = [ 1, 2, 3 ]"));
        assertNext(tokenizer, TomlToken.STRING, "integers");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.SQUAREOPEN);
        assertNext(tokenizer, TomlToken.INTEGER, 1);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.INTEGER, 2);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.INTEGER, 3);
        assertNext(tokenizer, TomlToken.SQUARECLOSE);
    }

}
