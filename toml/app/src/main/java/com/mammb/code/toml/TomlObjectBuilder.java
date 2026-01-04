package com.mammb.code.toml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.mammb.code.toml.TomlValue.*;

public class TomlObjectBuilder {

    private Map<String, TomlValue> valueMap;

    public TomlObjectBuilder() {
        this.valueMap = new LinkedHashMap<>();
    }
    public TomlObjectBuilder add(String key, TomlValue value) {
        return this;
    }
    public TomlObject build() {
        Map<String, TomlValue> snapshot = (valueMap == null)
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(valueMap);
        valueMap = new LinkedHashMap<>();
        return new TomlObject(snapshot);
    }

}
