package com.mammb.code.toml.impl;

import com.mammb.code.toml.TomlParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

class TomlParserImpl implements TomlParser {

    private final TomlTokenizer tokenizer;
    private boolean closed = false;

    TomlParserImpl(Reader reader, BufferPool bufferPool) {
        this.tokenizer = new TomlTokenizer(reader, bufferPool);
    }

    TomlParserImpl(InputStream in, BufferPool bufferPool) {
        this(new InputStreamReader(in), bufferPool);
    }

    @Override
    public void close() {
        if (closed) return;
        try {
            tokenizer.close();
            closed = true;
        } catch (IOException e) {
            throw new RuntimeException("I/O error while closing TOML tokenizer", e);
        }
    }

}
