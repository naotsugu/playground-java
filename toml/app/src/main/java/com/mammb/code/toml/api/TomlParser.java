package com.mammb.code.toml.api;

import java.io.Closeable;
import java.math.BigDecimal;
import com.mammb.code.toml.api.TomlValue.*;

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
    Event currentEvent();
    String getString();
    int getInt();
    long getLong();
    BigDecimal getBigDecimal();
    TomlArray getArray();
    TomlObject getObject();
    @Override
    void close();
}
