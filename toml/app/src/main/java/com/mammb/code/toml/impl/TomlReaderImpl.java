package com.mammb.code.toml.impl;

import com.mammb.code.toml.TomlParser;
import com.mammb.code.toml.TomlReader;
import java.io.IOException;
import java.io.InputStream;

public class TomlReaderImpl implements TomlReader {

    private final BufferPool bufferPool;
    private final TomlParser parser;
    private boolean readDone;

    public TomlReaderImpl(InputStream in) {
        bufferPool = BufferPool.defaultPool();
        parser = new TomlParserImpl(in, bufferPool);
    }

    @Override
    public void close() throws IOException {
        readDone = true;
        parser.close();
    }

}
