package org.example;

import java.util.List;
import java.util.Map;

public class App {

    static void main() {
        String toml = """
            title = "TOML Example"

            [owner]
            name = "Tom Preston-Werner"
            dob = 1979-05-27T07:32:00

            [database]
            server = "192.168.1.1"
            ports = [ 8001, 8001, 8002 ]
            connection_max = 5000
            enabled = true

            [servers]
              [servers.alpha]
              ip = "10.0.0.1"
              dc = "eqdc10"

              [servers.beta]
              ip = "10.0.0.2"
              dc = "eqdc10"

            [[products]]
            name = "Hammer"
            sku = 738594937

            [[products]]
            name = "Nail"
            sku = 284758393
            color = "gray"
            """;

        Map<String, Object> result = new Toml(toml).parse();
        printMap(result, 0);
    }

    @SuppressWarnings("unchecked")
    private static void printMap(Map<String, Object> map, int indent) {
        String pad = "  ".repeat(indent);
        for (var entry : map.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Map) {
                System.out.println(pad + entry.getKey() + ":");
                printMap((Map<String, Object>) val, indent + 1);
            } else if (val instanceof List<?> list) {
                System.out.println(pad + entry.getKey() + " (Array):");
                for (Object o : list) {
                    if (o instanceof Map) {
                        System.out.println(pad + "  - {");
                        printMap((Map<String, Object>) o, indent + 2);
                        System.out.println(pad + "    }");
                    } else {
                        System.out.println(pad + "  - " + o);
                    }
                }
            } else {
                System.out.println(pad + entry.getKey() + " = " + val + " (" + val.getClass().getSimpleName() + ")");
            }
        }
    }
}
