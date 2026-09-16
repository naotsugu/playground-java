package org.example;

import com.example.rust_lib.rust_lib_h;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

public class App extends Application {

    // Set so that width * 4 is a multiple of 256 (e.g., 1024 * 4 = 4096 = 256 * 16)
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;

    private Arena arena;
    private MemorySegment ctxPtr;
    private AnimationTimer timer;

    @Override
    public void start(Stage stage) {
        arena = Arena.ofShared();

        // initialize the Vello context on the Rust side and receive its pointer
        ctxPtr = rust_lib_h.vello_ctx_create(WIDTH, HEIGHT);
        if (ctxPtr.equals(MemorySegment.NULL)) {
            throw new RuntimeException("Failed to initialize Vello context");
        }

        // allocate a shared memory region for image transfer
        long bufferSize = (long) WIDTH * HEIGHT * 4;
        MemorySegment pixelData = arena.allocate(bufferSize);

        // set up PixelBuffer and WritableImage
        ByteBuffer byteBuffer = pixelData.asByteBuffer();
        PixelFormat<ByteBuffer> format = PixelFormat.getByteBgraPreInstance();
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, byteBuffer, format);
        WritableImage image = new WritableImage(pixelBuffer);

        ImageView imageView = new ImageView(image);
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        stage.setTitle("Vello Font Rendering");
        stage.setScene(scene);
        stage.show();

        rust_lib_h.vello_ctx_render(ctxPtr, pixelData, arena.allocateFrom("Hello"));
        pixelBuffer.updateBuffer(_ -> null);
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
        if (ctxPtr != null && !ctxPtr.equals(MemorySegment.NULL)) {
            rust_lib_h.vello_ctx_destroy(ctxPtr);
        }
        if (arena != null) arena.close();
    }

}
