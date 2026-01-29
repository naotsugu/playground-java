package com.mammb.code.toml.impl;

import com.mammb.code.toml.TomlParser;
import com.mammb.code.toml.TomlValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

class TomlParserImpl implements TomlParser {

    private final TomlTokenizer tokenizer;
    private final ContextStack stack;

    private TomlParserImpl(TomlTokenizer tokenizer, ContextStack stack) {
        this.tokenizer = tokenizer;
        this.stack = stack;
    }

    public TomlParserImpl(Reader reader, BufferPool bufferPool) {
        this(new TomlTokenizer(reader, bufferPool), new ContextStack(1000));
    }

    public TomlParserImpl(InputStream in, BufferPool bufferPool) {
        this(new InputStreamReader(in, StandardCharsets.UTF_8), bufferPool);
    }

    @Override
    public boolean hasNext() {
        // TODO
        return true;
    }

    @Override
    public Event next() {
        // TODO
        return null;
    }

    @Override
    public TomlValue.TomlObject getObject() {
        // TODO
        return null;
    }

    @Override
    public void close() throws IOException {
        // TODO
    }
}
