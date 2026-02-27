package org.example;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import java.awt.*;

public class App {

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        // FlatLightLaf.setup();
        // FlatMacDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new App().run());
    }

    private void run() {

        final JPanel panel = new JPanel();
        panel.add(new JTextField());
        panel.add(new JButton("OK"));

        JFrame frame = new JFrame("Hello FlatLaf");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        frame.add(panel);
        frame.setVisible(true);
    }

}
