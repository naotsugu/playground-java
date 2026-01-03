package com.mammb.code.toml;

import com.mammb.code.toml.impl.TomlReaderImpl;
import java.io.InputStream;

public final class Toml {

    private Toml() { }

    public static TomlReader createReader(InputStream in) {
        return new TomlReaderImpl(in);
    }

}
