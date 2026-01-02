package com.mammb.code.toml.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface TomlValue {

    interface TomlStructure extends TomlValue { }
    interface TomlArray extends TomlStructure, List<TomlValue> { }
    interface TomlObject extends TomlStructure, Map<String, TomlValue> { }

    interface TomlInteger extends TomlValue { }
    interface TomlFloat extends TomlValue { }
    interface TomlString extends TomlValue { }

    enum ValueType {
        ARRAY, OBJECT, STRING, INTEGER, FLOAT, TRUE, FALSE,
        ;
    }

    TomlValue TRUE = new TomlValueRecord(ValueType.TRUE);
    TomlValue FALSE = new TomlValueRecord(ValueType.FALSE);

    ValueType valueType();

    record TomlValueRecord(ValueType valueType) implements TomlValue, Serializable { }

}
