package org.example;

import javax.swing.*;
import java.awt.*;

public class App {

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.appearance", "system");
        SwingUtilities.invokeLater(() -> new App().run());
    }

    private void run() {
        JFrame frame = new JFrame("Hello World");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        JPanel canvas = new JPanel() {
            @Override public void paintComponent(Graphics g) {
                super.paintComponent(g);
                App.this.paint(g);
            }
        };
        canvas.setBackground(Color.BLACK);

        frame.add(canvas);
        frame.setVisible(true);
    }

    private void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font font1 = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        g.setFont(font1);
        g.setColor(Color.WHITE);
        g.drawString("AWT Canvas!", 20, 20);
    }

}
