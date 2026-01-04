package com.mammb.code.toml.impl;

import com.mammb.code.toml.TomlParser;
import com.mammb.code.toml.TomlReader;
import com.mammb.code.toml.TomlValue.*;
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

    public TomlObject readObject() {
        if (readDone) throw new IllegalStateException("readObject method is already called.");
        readDone = true;
        if (parser.hasNext()) {
            try {
                parser.next();
                return parser.getObject();
            } catch (IllegalStateException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Internal Error");
    }

    @Override
    public void close() throws IOException {
        readDone = true;
        parser.close();
    }

}
