package net.legacylauncher.ui.components;

import lombok.Setter;
import net.legacylauncher.ui.images.Images;
import net.legacylauncher.ui.swing.extended.ExtendedButton;
import net.legacylauncher.util.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * A bell-shaped icon button that displays a notification badge/counter.
 * Used to indicate available launcher updates.
 * <p>
 * Draws a bell icon programmatically using Java2D (no external image needed)
 * and overlays a red badge with the count when updates are available.
 */
public class BellIconButton extends ExtendedButton {

    private static final int BELL_SIZE = 24;
    private static final int BADGE_RADIUS = 8;

    private volatile boolean hasUpdate;
    private volatile int updateCount;

    @Setter
    private Color bellColor;
    @Setter
    private Color badgeColor;
    @Setter
    private Color badgeTextColor;

    private boolean hovered;
    private boolean pressed;

    public BellIconButton() {
        setPreferredSize(SwingUtil.magnify(new Dimension(40, 40)));
        setMinimumSize(SwingUtil.magnify(new Dimension(36, 36)));
        setMaximumSize(SwingUtil.magnify(new Dimension(48, 48)));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);

        bellColor = new Color(180, 180, 200);
        badgeColor = new Color(255, 60, 60);
        badgeTextColor = Color.WHITE;

        // Hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    /**
     * Sets whether an update is available and triggers a repaint.
     */
    public void setHasUpdate(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
        repaint();
    }

    /**
     * Sets the update count badge value.
     */
    public void setUpdateCount(int count) {
        this.updateCount = count;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // Scale factor for the bell
        double scale = Math.min(w, h) / 48.0;

        // Draw bell shape
        g2d.translate(cx, cy);
        g2d.scale(scale, scale);

        // Choose color based on state
        Color baseColor = bellColor;
        if (pressed) {
            baseColor = baseColor.darker();
        } else if (hovered || hasUpdate) {
            baseColor = baseColor.brighter();
        }

        g2d.setColor(baseColor);
        g2d.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Draw bell body using a Path2D
        Path2D.Double bellPath = new Path2D.Double();
        // Bell top curve (rounded dome)
        bellPath.moveTo(-10, -6);
        // Left side of bell
        bellPath.curveTo(-10, -14, -4, -18, 0, -18);
        bellPath.curveTo(4, -18, 10, -14, 10, -6);
        // Bottom edge of bell
        bellPath.lineTo(10, -2);
        bellPath.curveTo(10, 2, 14, 4, 14, 6);
        bellPath.lineTo(-14, 6);
        bellPath.curveTo(-14, 4, -10, 2, -10, -2);
        bellPath.closePath();

        // Fill bell body
        g2d.fill(bellPath);

        // Draw bell outline
        g2d.setColor(baseColor.darker());
        g2d.draw(bellPath);

        // Draw the clapper (ball at bottom)
        int clapperY = 8;
        g2d.fillOval(-3, clapperY, 6, 6);
        g2d.setColor(baseColor.darker());
        g2d.drawOval(-3, clapperY, 6, 6);

        // Draw the bell top knob
        g2d.fillOval(-2, -20, 4, 4);
        g2d.setColor(baseColor.darker());
        g2d.drawOval(-2, -20, 4, 4);

        g2d.scale(1 / scale, 1 / scale);
        g2d.translate(-cx, -cy);

        // Draw badge if update is available
        if (hasUpdate) {
            int badgeX = w - BADGE_RADIUS - 2;
            int badgeY = BADGE_RADIUS;

            // Badge circle with shadow
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillOval(badgeX + 1, badgeY + 1, BADGE_RADIUS * 2, BADGE_RADIUS * 2);

            g2d.setColor(badgeColor);
            g2d.fillOval(badgeX, badgeY, BADGE_RADIUS * 2, BADGE_RADIUS * 2);

            // Badge border
            g2d.setColor(badgeColor.brighter());
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(badgeX, badgeY, BADGE_RADIUS * 2, BADGE_RADIUS * 2);

            // Badge text (count)
            if (updateCount > 0) {
                String countStr = updateCount > 99 ? "99+" : String.valueOf(updateCount);
                g2d.setColor(badgeTextColor);
                g2d.setFont(getFont().deriveFont(Font.BOLD, 10f));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = badgeX + BADGE_RADIUS - fm.stringWidth(countStr) / 2;
                int textY = badgeY + BADGE_RADIUS + fm.getAscent() / 2 - 1;
                g2d.drawString(countStr, textX, textY);
            } else {
                // Just a dot (no number)
                g2d.setColor(badgeTextColor);
                g2d.fillOval(badgeX + 4, badgeY + 4, 4, 4);
            }
        }

        g2d.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        return SwingUtil.magnify(new Dimension(40, 40));
    }

    @Override
    public Dimension getMinimumSize() {
        return SwingUtil.magnify(new Dimension(36, 36));
    }

    @Override
    public Dimension getMaximumSize() {
        return SwingUtil.magnify(new Dimension(48, 48));
    }
}
