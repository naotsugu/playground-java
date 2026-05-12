package com.mammb.fswatch;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RecursiveWatchService {

    static void run(Path watchPath, Event.Listener listener) throws Exception {

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

            registerAll(watchService, watchPath);

            for (;;) {

                WatchKey watchKey = watchService.take();
                Path dir = (Path) watchKey.watchable();
                List<Event> events = new ArrayList<>();

                for (WatchEvent<?> event : watchKey.pollEvents()) {

                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path path = dir.resolve((Path) event.context());
                    boolean isDirectory = Files.isDirectory(path);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        if (isDirectory) {
                            events.add(new Event.DirectoryCreate(path));
                            registerAll(watchService, path);
                        } else {
                            events.add(new Event.FileCreate(path));
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        events.add(isDirectory
                            ? new Event.DirectoryDelete(path)
                            : new Event.FileDelete(path));
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        events.add(isDirectory
                            ? new Event.DirectoryChange(path)
                            : new Event.FileChange(path));
                    }
                }
                if (!Files.exists(dir)) {
                    watchKey.cancel();
                } else {
                    watchKey.reset();
                }
                events.forEach(listener);
            }
        }
    }

    private static void registerAll(WatchService watchService, Path path) {
        try (Stream<Path> stream = Files.find(path, Integer.MAX_VALUE,
            (_, a) -> a.isDirectory())) {
            stream.forEach(p -> {
                try {
                    p.register(watchService,
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_DELETE,
                        StandardWatchEventKinds.ENTRY_MODIFY);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
