package net.legacylauncher.ui.swing;

import net.legacylauncher.ui.LegacyLauncherFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A panel with glassmorphism effect: semi-transparent background with blur-like appearance,
 * rounded corners, and a subtle border glow. Ideal for overlaying on anime backgrounds.
 */
public class GlassPanel extends JPanel {
    private float alpha;
    private Color tintColor;
    private int arc;
    private boolean showBorder;

    public GlassPanel() {
        this(0.35f, new Color(20, 15, 40, 160));
    }

    public GlassPanel(float alpha, Color tintColor) {
        this.alpha = alpha;
        this.tintColor = tintColor;
        this.arc = 16;
        this.showBorder = true;
        setOpaque(false);
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        repaint();
    }

    public void setTintColor(Color color) {
        this.tintColor = color;
        repaint();
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    public void setShowBorder(boolean show) {
        this.showBorder = show;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();
        Shape shape = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);

        // Semi-transparent background for frosted glass effect
        g2d.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2d.setColor(tintColor);
        g2d.fill(shape);

        // Subtle border (glass edge highlight)
        if (showBorder) {
            g2d.setComposite(AlphaComposite.SrcOver.derive(0.25f));
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, arc, arc));
        }

        g2d.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (d.width < 80) d.width = 80;
        if (d.height < 40) d.height = 40;
        return d;
    }
}
