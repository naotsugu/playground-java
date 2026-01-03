package com.mammb.code.toml2.api;

import com.mammb.code.toml2.spi.TomlProvider;
import java.io.InputStream;

public final class Toml {
    private Toml() { }

    public static TomlReader createReader(InputStream in) {
        return TomlProvider.provider().createReader(in);
    }
}
