package net.legacylauncher.ui.background;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class TransparentBackground extends JComponent implements ISwingBackground {
    private boolean active;

    public TransparentBackground() {
        setOpaque(false);
    }

    @Override
    public void onResize() {
        if (getParent() != null) {
            setSize(getParent().getSize());
        }
    }

    @Override
    public void startBackground() {
        active = true;
        repaint();
    }

    @Override
    public void pauseBackground() {
        active = false;
    }

    @Override
    public void loadBackground(String path) throws Exception {
        // Transparent mode doesn't need a background path
        // It just makes the background transparent
        setOpaque(false);
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        // Draw nothing - fully transparent
        // The OS desktop will show through if the window supports translucency
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 0.0f));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.dispose();
    }

    public void wipe() {
        repaint();
    }
}
