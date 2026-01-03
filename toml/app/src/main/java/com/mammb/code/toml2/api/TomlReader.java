package com.mammb.code.toml2.api;

import java.io.Closeable;
import com.mammb.code.toml2.api.TomlValue.*;

public interface TomlReader extends Closeable {

    TomlStructure read();

    TomlObject readObject();

    TomlArray readArray();

    default TomlValue readValue() {
        throw new UnsupportedOperationException();
    }

    void close();
}
