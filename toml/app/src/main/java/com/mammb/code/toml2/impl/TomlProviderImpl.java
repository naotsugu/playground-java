package com.mammb.code.toml2.impl;

import com.mammb.code.toml2.api.TomlReader;
import com.mammb.code.toml2.spi.TomlProvider;
import java.io.InputStream;

public class TomlProviderImpl extends TomlProvider {

    private final BufferPool bufferPool = BufferPool.defaultPool();
    private final TomlContext emptyContext = new TomlContext(null, bufferPool);

    @Override
    public TomlReader createReader(InputStream in) {
        return new TomlReaderImpl(in, emptyContext);
    }

}
