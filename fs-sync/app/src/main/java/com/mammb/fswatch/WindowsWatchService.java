import java.nio.file.*;
import com.sun.nio.file.ExtendedWatchEventModifier;
import static java.lang.IO.println;

void main() throws Exception {

    Path watchPath = Path.of(".");

    try (WatchService watchService = FileSystems.getDefault().newWatchService()) {

        WatchEvent.Kind<?>[] kinds = new WatchEvent.Kind<?>[] {
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        };
        WatchEvent.Modifier[] modifiers = new WatchEvent.Modifier[] {
            // windows only
            ExtendedWatchEventModifier.FILE_TREE
        };
        watchPath.register(watchService, kinds, modifiers);
        println("watchPath : " + watchPath);

        for (;;) {

            WatchKey watchKey = watchService.take();
            Path dir = (Path) watchKey.watchable();

            for (WatchEvent<?> event : watchKey.pollEvents()) {

                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    println("OVERFLOW");
                    continue;
                }

                Path path = dir.resolve((Path) event.context());

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    println("CREATE : " + path);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    println("DELETE : " + path);
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    println("MODIFY : " + path);
                } else {
                    println("unknown kind: " + kind);
                }
            }

            boolean valid = watchKey.reset();
            if (!valid) {
                break;
            }
        }
    }
}
