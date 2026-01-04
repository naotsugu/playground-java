package com.mammb.code.toml;

import java.io.Closeable;
import com.mammb.code.toml.TomlValue.*;

public interface TomlParser extends Closeable {

    enum Event {
        START_ARRAY,
        START_OBJECT,
        KEY_NAME,
        VALUE_STRING,
        VALUE_INTEGER,
        VALUE_FLOAT,
        VALUE_OFFSET_DATETIME,
        VALUE_LOCAL_DATETIME,
        VALUE_LOCAL_DATE,
        VALUE_LOCAL_TIME,
        VALUE_TRUE,
        VALUE_FALSE,
        END_OBJECT,
        END_ARRAY
    }

    boolean hasNext();
    Event next();
    TomlObject getObject();
}
