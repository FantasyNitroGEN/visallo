package org.visallo.quickStart.gui;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;

public class TextAreaOutputStreamAdapter extends OutputStream {
    public static final int BUFFER_SIZE = 50000;
    private final JTextArea textArea;
    private final StringBuilder sb = new StringBuilder();

    public TextAreaOutputStreamAdapter(final JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\r') {
            return;
        }

        if (b == '\n') {
            final String text = sb.toString() + "\n";
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String currentText = textArea.getText();
                    if (currentText.length() > BUFFER_SIZE) {
                        textArea.setText(currentText.substring(currentText.length() - BUFFER_SIZE));
                    }
                    textArea.append(text);
                }
            });
            sb.setLength(0);
            return;
        }

        sb.append((char) b);
    }
}
