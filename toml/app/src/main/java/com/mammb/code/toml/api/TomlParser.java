package com.mammb.code.toml.api;

import java.io.Closeable;

public interface TomlParser extends Closeable {
    enum Event {
        START_ARRAY,
        START_OBJECT,
        KEY_NAME,
        VALUE_STRING,
        VALUE_INTEGER,
        VALUE_FLOAT,
        VALUE_TRUE,
        VALUE_FALSE,
        END_OBJECT,
        END_ARRAY
    }

    boolean hasNext();
    Event next();
    String getString();
    @Override
    void close();
}
