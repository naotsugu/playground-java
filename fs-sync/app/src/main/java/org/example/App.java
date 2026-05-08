package org.example;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;
import java.io.File;
import java.nio.file.Paths;

public class App {

    public static void main(String[] args) throws Exception {

        FileEntry directory = new FileEntry(Paths.get(".").resolve("src").toFile());

        FileAlterationObserver observer = FileAlterationObserver.builder()
            .setRootEntry(directory)
            .setFileFilter(f -> f.getName().endsWith(".java"))
            .get();

        observer.addListener(new FileAlterationListenerAdaptor() {
            @Override
            public void onFileCreate(File file) {
                // code for processing creation event
            }

            @Override
            public void onFileDelete(File file) {
                // code for processing deletion event
            }

            @Override
            public void onFileChange(File file) {
                // code for processing change event
            }
        });

        FileAlterationMonitor monitor = new FileAlterationMonitor();
        monitor.addObserver(observer);
        monitor.start();

    }
}
