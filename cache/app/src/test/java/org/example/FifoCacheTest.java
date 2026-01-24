package org.example;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FifoCacheTest {

    @Test
    void test() {
        var fifo = new FifoCache<String, String>(5);

        for (int i = 0; i < 10; i++) {
            fifo.put("key" + i, "value" + i);
        }

        assertEquals(5, fifo.size());

        int i = 5;
        for (var entry : fifo.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            assertEquals("key" + i, entry.getKey());
            assertEquals("value" + (i++), entry.getValue());
        }

    }
}
