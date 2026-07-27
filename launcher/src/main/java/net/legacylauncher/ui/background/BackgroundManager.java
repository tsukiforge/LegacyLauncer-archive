package net.legacylauncher.ui.background;

import lombok.extern.slf4j.Slf4j;
import net.legacylauncher.LegacyLauncher;
import net.legacylauncher.configuration.Configuration;
import net.legacylauncher.ui.MainPane;
import net.legacylauncher.ui.background.fx.FxAudioPlayer;
import net.legacylauncher.ui.background.fx.MediaFxBackground;
import net.legacylauncher.ui.images.Images;
import net.legacylauncher.ui.images.ResourceNotFoundException;
import net.legacylauncher.ui.swing.extended.ExtendedLayeredPane;
import net.legacylauncher.util.Lazy;
import net.legacylauncher.util.shared.JavaVersion;

import javax.swing.*;
import java.net.URL;

@Slf4j
public final class BackgroundManager extends ExtendedLayeredPane {
    private final static int BACKGROUND_INDEX = 1, COVER_INDEX = Integer.MAX_VALUE;
    final Worker worker;
    final Cover cover;

    private final Lazy<ImageBackground> imageBackground;
    public final Lazy<OldAnimatedBackground> oldBackground;
    private final FXWrapper<MediaFxBackground> mediaFxBackground;
    private final Lazy<AnimeBackground> animeBackground;
    private final Lazy<TransparentBackground> transparentBackground;

    private IBackground background;
    private String previousMode;

    public BackgroundManager(MainPane pane) {
        super(pane);

        worker = new Worker(this);

        cover = new Cover();
        add(cover, COVER_INDEX);

        imageBackground = Lazy.of(ImageBackground::new);
        oldBackground = Lazy.of(OldAnimatedBackground::new);
        animeBackground = Lazy.of(AnimeBackground::new);
        transparentBackground = Lazy.of(TransparentBackground::new);
        FXWrapper<MediaFxBackground> _mediaFxBackground = null;
        try {
            if (JavaVersion.getCurrent().getMajor() >= 11) {
                _mediaFxBackground = new FXWrapper<>(MediaFxBackground.class);
            } else {
                log.info("MediaFxBackground is not be available because it requires Java 11+");
            }
        } catch (Throwable t) {
            log.info("MediaFxBackground will not be available: {}", t.toString());
            log.debug("Detailed exception", t);
        }
        mediaFxBackground = _mediaFxBackground;
    }

    public boolean isMediaFxAvailable() {
        return mediaFxBackground != null;
    }

    void setBackground(IBackground background) {
        if (this.background == background) {
            return;
        }

        if (this.background != null) {
            this.background.pauseBackground();
            remove((JComponent) this.background);
        }

        if (background != null) {
            add((JComponent) background, BACKGROUND_INDEX);
            background.startBackground();
        }

        this.background = background;
        onResize();
    }

    public void startBackground() {
        if (background != null) {
            background.startBackground();
        }
    }

    public void pauseBackground() {
        if (background != null) {
            background.pauseBackground();
        }
    }

    public void loadBackground() {
        Configuration settings = LegacyLauncher.getInstance().getSettings();
        String previousMode = this.previousMode;
        String mode = settings.get("gui.background.mode");
        if (mode == null) mode = "anime";
        this.previousMode = mode;

        // Disable transparency when switching away from transparent mode
        if (previousMode != null && "transparent".equals(previousMode) && !"transparent".equals(mode)) {
            SwingUtilities.invokeLater(() ->
                LegacyLauncher.getInstance().getFrame().disableTransparency()
            );
        }

        switch (mode) {
            case "video": {
                String path = settings.get("gui.background");
                if (path != null && mediaFxBackground != null && (path.endsWith(".mp4") || path.endsWith(".flv"))) {
                    worker.setBackground(mediaFxBackground, path);
                } else if (nostalgic) {
                    OldAnimatedBackground nostalgicBackground = oldBackground.get();
                    nostalgicBackground.getAudioPlayer().value().ifPresent(FxAudioPlayer::play);
                    worker.setBackground(nostalgicBackground, path);
                } else {
                    worker.setBackground(imageBackground.get(), path);
                }
                break;
            }
            case "transparent": {
                worker.setBackground(transparentBackground.get(), null);
                // Enable per-pixel translucency on the frame so desktop shows through
                SwingUtilities.invokeLater(() ->
                    LegacyLauncher.getInstance().getFrame().enableTransparency()
                );
                break;
            }
            default: { // anime mode
                String animeUrl = settings.get("gui.background.anime.url");
                if (animeUrl == null || animeUrl.isEmpty()) {
                    animeUrl = "https://images.unsplash.com/photo-1547954575-855750c57bd3?auto=format&fit=crop&w=1920&q=80";
                }
                worker.setBackground(animeBackground.get(), animeUrl);
                break;
            }
        }
    }

    private boolean nostalgic;

    public void setNostalgic(boolean state) {
        this.nostalgic = state;
        this.loadBackground();
    }
}
