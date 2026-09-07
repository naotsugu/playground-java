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

    // 幅 * 4 が 256 の倍数になるように設定 (1024 * 4 = 4096 = 256 * 16)
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;

    private Arena arena;

    @Override
    public void start(Stage stage) {
        arena = Arena.ofShared();

        long bufferSize = (long) WIDTH * HEIGHT * 4;
        MemorySegment segment = arena.allocate(bufferSize);

        System.out.println("[Java] Vello (GPU) でシーンをレンダリングします...");

        // ★ 自動生成された render_vello_scene 関数を呼び出す
        rust_lib_h.render_vello_scene(segment, WIDTH, HEIGHT);

        ByteBuffer byteBuffer = segment.asByteBuffer();

        // BGRA事前乗算形式でバッファを作成
        PixelFormat<ByteBuffer> format = PixelFormat.getByteBgraPreInstance();
        PixelBuffer<ByteBuffer> pixelBuffer = new PixelBuffer<>(WIDTH, HEIGHT, byteBuffer, format);
        WritableImage image = new WritableImage(pixelBuffer);

        ImageView imageView = new ImageView(image);
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        stage.setTitle("JavaFX + Vello(GPU) Zero-Copy Rendering");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (arena != null) {
            arena.close();
        }
    }

}
