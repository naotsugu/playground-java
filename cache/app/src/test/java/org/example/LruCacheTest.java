package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LruCacheTest {

    @Test
    void test() {
        var lru = new LruCache<String, String>(5);

        for (int i = 0; i < 10; i++) {
            lru.put("key" + i, "value" + i);
        }

        assertEquals(5, lru.size());

        int i = 5;
        for (var entry : lru.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            assertEquals("key" + i, entry.getKey());
            assertEquals("value" + (i++), entry.getValue());
        }

        lru.get("key7");
        lru.put("keyA", "valueA");

        assertIterableEquals(
            List.of("key6", "key7", "key8", "key9", "keyA"),
            lru.entrySet().stream().map(Map.Entry::getKey).sorted().toList());

        lru.get("key9");
        lru.put("keyB", "valueB");
        lru.put("keyC", "valueC");

        assertIterableEquals(
            List.of("key7", "key9", "keyA", "keyB", "keyC"),
            lru.entrySet().stream().map(Map.Entry::getKey).sorted().toList());

    }

}
