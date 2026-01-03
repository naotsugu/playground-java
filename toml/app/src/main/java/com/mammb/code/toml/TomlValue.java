package com.mammb.code.toml;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface TomlValue {

    interface TomlStructure extends TomlValue { }

    record TomlArray(List<TomlValue> valueList) implements TomlStructure { }
    record TomlObject(Map<String, TomlValue> valueMap) implements TomlStructure { }
    record TomlString(CharSequence value) implements TomlValue { }
    record TomlInteger(int value) implements TomlValue { }
    record TomlFloat(double value) implements TomlValue { }
    record TomlBoolean(boolean value) implements TomlValue { }
    record TomlDateTime(OffsetDateTime value) implements TomlValue { }
    record TomlLocalDateTime(LocalDateTime value) implements TomlValue { }
    record TomlLocalDate(LocalDate value) implements TomlValue { }
    record TomlLocalTime(LocalTime value) implements TomlValue { }

    TomlValue TRUE = new TomlBoolean(true);
    TomlValue FALSE = new TomlBoolean(false);
    TomlArray EMPTY_ARRAY = new TomlArray(Collections.emptyList());
    TomlObject EMPTY_OBJECT = new TomlObject(Collections.emptyMap());

}
