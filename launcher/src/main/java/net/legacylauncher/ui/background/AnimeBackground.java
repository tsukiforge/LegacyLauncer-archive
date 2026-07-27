package net.legacylauncher.ui.background;

import lombok.extern.slf4j.Slf4j;
import net.legacylauncher.LegacyLauncher;
import net.legacylauncher.ui.swing.extended.ExtendedComponentAdapter;
import net.legacylauncher.util.SwingUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class AnimeBackground extends JComponent implements ISwingBackground {
    private static final String CACHE_DIR = "anime_cache";
    private static final long MAX_CACHE_AGE = 24 * 60 * 60 * 1000L; // 24 hours
    private static final int MAX_FILE_SIZE = 1024 * 1024 * 8; // 8MB

    private Image loadedImage;
    private Image scaledImage;
    private String currentUrl;
    private boolean loading = false;

    public AnimeBackground() {
        addComponentListener(new ExtendedComponentAdapter(this) {
            @Override
            public void onComponentResized() {
                rescaleImage();
            }
        });
    }

    @Override
    public void onResize() {
        if (getParent() != null) {
            setSize(getParent().getSize());
            rescaleImage();
        }
    }

    @Override
    public void startBackground() {
        rescaleImage();
        repaint();
    }

    @Override
    public void pauseBackground() {
        // no-op for static images
    }

    @Override
    public void loadBackground(String path) throws Exception {
        if (path == null || path.isEmpty()) {
            loadedImage = null;
            scaledImage = null;
            repaint();
            return;
        }

        if (loading || path.equals(currentUrl)) {
            return;
        }

        loading = true;
        currentUrl = path;

        CompletableFuture.runAsync(() -> {
            try {
                Image image = loadFromCacheOrUrl(path);
                if (image != null && path.equals(currentUrl)) {
                    SwingUtil.later(() -> {
                        loadedImage = image;
                        rescaleImage();
                        repaint();
                        loading = false;
                    });
                } else {
                    loading = false;
                }
            } catch (Exception e) {
                log.warn("Could not load anime background: {}", path, e);
                loading = false;
            }
        });
    }

    private Image loadFromCacheOrUrl(String urlStr) throws Exception {
        // Try loading from cache first
        String cacheKey = String.valueOf(urlStr.hashCode());
        Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), CACHE_DIR);

        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }

        Path cacheFile = cacheDir.resolve(cacheKey + ".img");

        if (Files.exists(cacheFile)) {
            long fileAge = System.currentTimeMillis() - Files.getLastModifiedTime(cacheFile).toMillis();
            if (fileAge < MAX_CACHE_AGE) {
                try (InputStream in = new FileInputStream(cacheFile.toFile())) {
                    Image cached = ImageIO.read(in);
                    if (cached != null) {
                        log.debug("Loaded anime background from cache: {}", urlStr);
                        return cached;
                    }
                } catch (Exception e) {
                    log.warn("Cache read failed, re-downloading", e);
                }
            }
        }

        // Download from URL
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);
        conn.connect();

        int contentLength = conn.getContentLength();
        if (contentLength > MAX_FILE_SIZE) {
            log.warn("Image too large: {} bytes (max: {})", contentLength, MAX_FILE_SIZE);
            return null;
        }

        try (InputStream in = conn.getInputStream()) {
            // Save to cache
            byte[] imageData = readAllBytes(in);

            if (imageData.length > MAX_FILE_SIZE) {
                log.warn("Downloaded image too large: {} bytes", imageData.length);
                return null;
            }

            // Write cache
            try (FileOutputStream fos = new FileOutputStream(cacheFile.toFile())) {
                fos.write(imageData);
            }

            // Read from cached file
            try (InputStream cacheIn = new FileInputStream(cacheFile.toFile())) {
                Image image = ImageIO.read(cacheIn);

                if (image == null) {
                    Files.deleteIfExists(cacheFile);
                    log.warn("Could not decode image from: {}", urlStr);
                }

                return image;
            }
        }
    }

    private void rescaleImage() {
        if (loadedImage == null || getWidth() <= 0 || getHeight() <= 0) {
            scaledImage = null;
            return;
        }

        int imgW = loadedImage.getWidth(null);
        int imgH = loadedImage.getHeight(null);

        if (imgW <= 0 || imgH <= 0) {
            return;
        }

        // Cover the entire area while maintaining aspect ratio
        double scale = Math.max(
                (double) getWidth() / imgW,
                (double) getHeight() / imgH
        );

        int newW = (int) (imgW * scale);
        int newH = (int) (imgH * scale);

        try {
            BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(loadedImage, 0, 0, newW, newH, null);
            g.dispose();
            scaledImage = scaled;
        } catch (OutOfMemoryError e) {
            log.warn("Out of memory while scaling anime background");
            scaledImage = null;
        }
    }

    @Override
    public void paint(Graphics g) {
        if (scaledImage == null && loadedImage == null) {
            // Draw a gradient fallback
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setPaint(new GradientPaint(
                    0, 0, new Color(147, 112, 219, 120),
                    getWidth(), getHeight(), new Color(72, 61, 139, 120)
            ));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();
            return;
        }

        if (scaledImage == null) {
            return;
        }

        int x = (getWidth() - scaledImage.getWidth(null)) / 2;
        int y = (getHeight() - scaledImage.getHeight(null)) / 2;

        g.drawImage(scaledImage, x, y, null);
    }

    public void wipe() {
        loadedImage = null;
        scaledImage = null;
        currentUrl = null;
        repaint();
    }

    /** Java 8-compatible replacement for InputStream.readAllBytes() */
    private static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int n;
        while ((n = in.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }
}
