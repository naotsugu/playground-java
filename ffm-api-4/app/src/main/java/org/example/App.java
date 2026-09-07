package org.example;

import com.example.rust_lib.rust_lib_h;
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

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Arena arena;

    @Override
    public void start(Stage stage) {

        arena = Arena.ofShared();

        long bufferSize = (long) WIDTH * HEIGHT * 4; // 4bytes[BGRA]
        MemorySegment segment = arena.allocate(bufferSize);

        // call rust lib
        rust_lib_h.render_pattern(segment, WIDTH, HEIGHT);

        // FFM API MemorySegment to ByteBuffer
        ByteBuffer byteBuffer = segment.asByteBuffer();

        // Specifying the BGRA pre-multiplied format triggers the fastest drawing path in JavaFX (Prism).
        PixelFormat<ByteBuffer> format = PixelFormat.getByteBgraPreInstance();
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, byteBuffer, format);
        WritableImage image = new WritableImage(pixelBuffer);

        ImageView imageView = new ImageView(image);
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        stage.setTitle("JavaFX + Rust FFM API - Zero-Copy PixelBuffer");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (arena != null) arena.close();
    }

}
