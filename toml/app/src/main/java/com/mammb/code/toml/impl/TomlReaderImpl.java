package com.mammb.code.toml.impl;

import com.mammb.code.toml.api.TomlReader;
import com.mammb.code.toml.api.TomlValue;
import java.io.InputStream;

public class TomlReaderImpl implements TomlReader {

    private final TomlParserImpl parser;
    private boolean readDone;

    TomlReaderImpl(InputStream in, TomlContext context) {
        parser = new TomlParserImpl(in, context);
    }

    @Override
    public TomlValue.TomlStructure read() {
        // TODO
        return null;
    }

    @Override
    public TomlValue.TomlObject readObject() {
        // TODO
        return null;
    }

    @Override
    public TomlValue.TomlArray readArray() {
        // TODO
        return null;
    }

    @Override
    public void close() {
        // TODO
    }
}
