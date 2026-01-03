package com.mammb.code.toml2.impl;

public interface TomlConfig {

    /**
     * Configuration property to limit the maximum level of nesting when being parsing JSON string.
     * Default value is set to {@code 1000}.
     */
    String MAX_DEPTH = "com.mammb.code.toml.impl.maxDepth";

}
