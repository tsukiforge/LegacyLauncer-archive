package net.legacylauncher.ui.swing;

import net.legacylauncher.ui.LegacyLauncherFrame;
import net.legacylauncher.ui.loc.LocalizableButton;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A button with rounded corners and anime-style neon colors.
 * Extends LocalizableButton for built-in translation support.
 * Supports various color presets: NEON_BLUE, SOFT_PINK, PURPLE_PASTEL, CYBER_GREEN.
 */
public class RoundedButton extends LocalizableButton {

    public enum AnimeColor {
        NEON_BLUE(new Color(0, 191, 255), new Color(0, 150, 220), new Color(0, 100, 180)),
        SOFT_PINK(new Color(255, 105, 180), new Color(230, 80, 160), new Color(200, 50, 140)),
        PURPLE_PASTEL(new Color(180, 140, 255), new Color(150, 110, 230), new Color(120, 80, 200)),
        CYBER_GREEN(new Color(0, 255, 170), new Color(0, 220, 140), new Color(0, 180, 110)),
        SUNSET_ORANGE(new Color(255, 160, 60), new Color(230, 130, 40), new Color(200, 100, 20));

        private final Color main, hover, pressed;

        AnimeColor(Color main, Color hover, Color pressed) {
            this.main = main;
            this.hover = hover;
            this.pressed = pressed;
        }

        public Color getMain() { return main; }
        public Color getHover() { return hover; }
        public Color getPressed() { return pressed; }
    }

    private static final int DEFAULT_ARC = 16;
    private AnimeColor animeColor;
    private int arc;
    private boolean isHovered;
    private boolean isPressed;
    private Color textColor = Color.WHITE;

    public RoundedButton() {
        this(AnimeColor.NEON_BLUE);
    }

    public RoundedButton(AnimeColor color) {
        this.animeColor = color;
        this.arc = DEFAULT_ARC;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(getFont().deriveFont(Font.BOLD, LegacyLauncherFrame.getFontSize() * 1.2f));
        setForeground(textColor);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public RoundedButton(String text) {
        this(text, AnimeColor.NEON_BLUE);
    }

    public RoundedButton(String text, AnimeColor color) {
        super(text);
        this.animeColor = color;
        this.arc = DEFAULT_ARC;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(getFont().deriveFont(Font.BOLD, LegacyLauncherFrame.getFontSize() * 1.2f));
        setForeground(textColor);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setAnimeColor(AnimeColor color) {
        this.animeColor = color;
        repaint();
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();
        Shape shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc);

        // Determine color based on state
        Color bgColor;
        if (isPressed) {
            bgColor = animeColor.getPressed();
        } else if (isHovered) {
            bgColor = animeColor.getHover();
        } else {
            bgColor = animeColor.getMain();
        }

        // Draw shadow
        if (isHovered) {
            g2d.setColor(new Color(0, 0, 0, 50));
            g2d.fill(new RoundRectangle2D.Float(2, 3, w - 3, h - 3, arc, arc));
        }

        // Draw main background with gradient
        g2d.setPaint(new GradientPaint(0, 0, bgColor, w, h, bgColor.darker()));
        g2d.fill(shape);

        // Draw subtle glow effect when hovered
        if (isHovered) {
            g2d.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 60));
            g2d.setStroke(new BasicStroke(2f));
            g2d.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, arc, arc));
        }

        g2d.dispose();

        // Let the button paint its text
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 255, 255, 40));
        g2d.setStroke(new BasicStroke(1f));
        g2d.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, arc, arc));
        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width += 24;
        d.height = Math.max(d.height + 8, 36);
        return d;
    }
}
