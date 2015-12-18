package org.visallo.quickStart.gui;

import org.visallo.core.exception.VisalloException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;

public class VisalloWindow {
    private VisalloPanel panel;

    public VisalloWindow(int visalloHttpPort) {
        JFrame frame = new JFrame("Visallo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = new VisalloPanel(visalloHttpPort);
        frame.getContentPane().add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static class VisalloPanel extends JPanel {
        private final TextAreaOutputStreamAdapter outputStream;

        public VisalloPanel(int visalloHttpPort) {
            setLayout(new BorderLayout());

            JTextArea textArea = new JTextArea(30, 120);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane, BorderLayout.NORTH);
            outputStream = new TextAreaOutputStreamAdapter(textArea);

            if (Desktop.isDesktopSupported()) {
                JLabel link = new JLabel();
                try {
                    final URI url = new URI("http://localhost:" + visalloHttpPort);
                    link.setText("<html>Open Visallo <a href=\"\">" + url.toString() + "</a></html>");
                    link.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    link.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            try {
                                Desktop.getDesktop().browse(url);
                            } catch (IOException ex) {
                                System.out.println("Could not launch URL: " + url);
                            }
                        }
                    });
                    add(link, BorderLayout.SOUTH);
                } catch (URISyntaxException e) {
                    throw new VisalloException("Could not create URI", e);
                }
            }
        }

        public OutputStream getOutputStream() {
            return outputStream;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        VisalloWindow window = new VisalloWindow(8080);
        System.setOut(new PrintStream(window.getOutputStream()));
        for (int i = 0; ; i++) {
            System.out.println("hello " + i);
            Thread.sleep(10);
        }
    }

    public OutputStream getOutputStream() {
        return panel.getOutputStream();
    }
}
