package org.example;

import java.io.BufferedInputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class App {

    public static void main(String[] args) throws Exception {
        var path = Measurements.FILE;
        if (!Files.exists(path)) {
            Measurements.crate(100_000_000);
        }

        System.out.println("\ncountLineWithIoStream");
        countLineWithIoStream(path);

        System.out.println("\ncountLineWithChannel");
        countLineWithChannel(path);

        System.out.println("\ncountLineWithMemorySegment");
        countLineWithMemorySegment(path);

        System.out.println("\ncountLineWithMemorySegmentParallel");
        countLineWithMemorySegmentParallel(path);

        System.out.println("\nEnter any key to exit..");
        System.in.read();

    }

    static void countLineWithIoStream(Path path) throws Exception {
        long start = System.currentTimeMillis();
        long lines = 0;
        try (var is = new BufferedInputStream(Files.newInputStream(path))) {
            int r;
            while ((r = is.read()) >= 0) {
                if (r == '\n') lines++;
            }
        }
        System.out.printf("%,d in %,d ms%n", lines, System.currentTimeMillis() - start); // 20,000 ms
    }

    static void countLineWithChannel(Path path) throws Exception {

        long start = System.currentTimeMillis();
        long lines = 0;

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate(1024 * 32);
            for (;;) {
                buf.clear();
                int n = channel.read(buf);
                if (n < 0) break;
                buf.flip();
                while (buf.hasRemaining()) {
                    if (buf.get() == '\n') lines++;
                }
            }
        }

        System.out.printf("%,d in %,d ms%n", lines, System.currentTimeMillis() - start); // 2,000 ms
    }


    static void countLineWithMemorySegment(Path path) throws Exception {

        long start = System.currentTimeMillis();
        long lines = 0;

        try (var arena = Arena.ofConfined();
             var channel = FileChannel.open(path, StandardOpenOption.READ)) {

            long length = channel.size();
            MemorySegment seg = channel.map(FileChannel.MapMode.READ_ONLY, 0, length, arena);

            for (long i = 0; i < length; i++) {
                if (seg.get(ValueLayout.JAVA_BYTE, i) == '\n') {
                    lines++;
                }
            }
        }
        System.out.printf("%,d in %,d ms%n", lines, System.currentTimeMillis() - start); // 2,000 ms
    }

    static void countLineWithMemorySegmentParallel(Path path) throws Exception {

        long start = System.currentTimeMillis();
        long lines;

        try (var arena = Arena.ofShared(); // parallel needs ofShared arena
             var channel = FileChannel.open(path, StandardOpenOption.READ)) {

            long length = channel.size();
            MemorySegment seg = channel.map(FileChannel.MapMode.READ_ONLY, 0, length, arena);

            lines = seg.elements(ValueLayout.JAVA_BYTE)
                    .parallel()
                    .filter(m -> m.get(ValueLayout.JAVA_BYTE, 0) == '\n')
                    .count();
        }
        System.out.printf("%,d in %,d ms%n", lines, System.currentTimeMillis() - start); // 500 ms
    }


}
