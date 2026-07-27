package net.legacylauncher.ui.scenes;

import net.legacylauncher.configuration.Configuration;
import net.legacylauncher.minecraft.auth.Account;
import net.legacylauncher.ui.MainPane;
import net.legacylauncher.ui.login.LoginForm;
import net.legacylauncher.ui.notice.MainNoticePanel;
import net.legacylauncher.ui.notice.Notice;
import net.legacylauncher.ui.notice.NoticeSidePanel;
import net.legacylauncher.managers.UpdateChecker;
import net.legacylauncher.ui.components.BellIconButton;
import net.legacylauncher.ui.loc.Localizable;
import net.legacylauncher.ui.notification.NotificationPanel;
import net.legacylauncher.ui.settings.SettingsPanel;
import net.legacylauncher.ui.swing.DelayedComponent;
import net.legacylauncher.ui.swing.DelayedComponentLoader;
import net.legacylauncher.ui.swing.GlassPanel;
import net.legacylauncher.ui.swing.extended.ExtendedPanel;
import net.legacylauncher.util.Direction;
import net.legacylauncher.util.SwingUtil;

import javax.swing.*;
import java.awt.*;

public class DefaultScene extends PseudoScene {
    public static final Dimension LOGIN_SIZE = new Dimension(285, 240);
    public static final Dimension SETTINGS_SIZE = new Dimension(600, 550);

    public final LoginForm loginForm;
    public final DelayedComponent<SettingsPanel> settingsForm;
    public final DelayedComponent<NoticeSidePanel> noticeSidePanel;
    private DefaultScene.SidePanel sidePanel;
    private ExtendedPanel sidePanelComp;
    private Direction lfDirection;
    public final DelayedComponent<MainNoticePanel> noticePanel;
    public final NotificationPanel notificationPanel;

    // Bell icon for update notifications
    public final BellIconButton bellIconButton;

    // Glassmorphism overlay for anime theme
    private final GlassPanel glassOverlay;

    public DefaultScene(MainPane main) {
        super(main);

        // Glassmorphism overlay for anime theme background
        glassOverlay = new GlassPanel(0.40f, new Color(10, 8, 30, 180));
        glassOverlay.setArc(16);
        glassOverlay.setShowBorder(true);
        glassOverlay.setVisible(true);
        add(glassOverlay);

        settingsForm = new DelayedComponent<>(new DelayedComponentLoader<SettingsPanel>() {
            @Override
            public SettingsPanel loadComponent() {
                return new SettingsPanel(DefaultScene.this);
            }

            @Override
            public void onComponentLoaded(SettingsPanel loaded) {
                loaded.setVisible(false);
                DefaultScene.this.add(loaded);
                loaded.setSize(SwingUtil.magnify(SETTINGS_SIZE));
                loaded.ready = true;
            }
        });
        //settingsForm.setVisible(false);
        //add(settingsForm);
        loginForm = new LoginForm(this);
        loginForm.setSize(SwingUtil.magnify(LOGIN_SIZE));
        add(loginForm);
        noticePanel = new DelayedComponent<>(new DelayedComponentLoader<MainNoticePanel>() {
            @Override
            public MainNoticePanel loadComponent() {
                return new MainNoticePanel(DefaultScene.this);
            }

            @Override
            public void onComponentLoaded(MainNoticePanel loaded) {
                DefaultScene.this.add(loaded);
                updateSidePanel();
            }
        });
        //add(noticePanel);
        noticeSidePanel = new DelayedComponent<>(new DelayedComponentLoader<NoticeSidePanel>() {
            @Override
            public NoticeSidePanel loadComponent() {
                return new NoticeSidePanel(DefaultScene.this);
            }

            @Override
            public void onComponentLoaded(NoticeSidePanel loaded) {
                DefaultScene.this.add(loaded);
                updateSidePanel();
            }
        });
        this.notificationPanel = new NotificationPanel();
        add(notificationPanel);

        // Bell icon button for update notifications (top-right corner)
        this.bellIconButton = new BellIconButton();
        bellIconButton.addActionListener(e -> onBellClicked());
        add(bellIconButton);

        updateDirection();
    }

    private void updateSidePanel() {
        if (isNoticeSidePanelEnabled() && getMainPane().getRootFrame().getNotices().getForCurrentLocale() != null) {
            setSidePanel(null);
        }
    }

    public void setShown(boolean shown, boolean animate) {
        super.setShown(shown, animate);
        if (shown) {
            if (getMainPane().getRootFrame().getNotices().getSelectedNotice() != null) {
                if (getMainPane().getRootFrame().getNotices().isHidden(noticePanel.get().getNotice())) {
                    getMainPane().getRootFrame().getNotices().selectRandom();
                }
                noticePanel.get().redraw();
            }

            if (getMainPane().accountManager.isLoaded()) {
                Account<?> selected = getMainPane().accountManager.get().list.getSelected();
                if (selected != null) {
                    loginForm.accounts.setAccount(selected);
                }
            }
        }
    }

    public void onResize() {
        if (parent != null) {
            setBounds(0, 0, parent.getWidth(), parent.getHeight());
            updateCoords();
        }
    }

    private static final int MARGIN = 10, SPACE_BETWEEN = 15;

    private void updateCoords() {
        final int margin = SwingUtil.magnify(MARGIN), spaceBetween = SwingUtil.magnify(SPACE_BETWEEN);

        int w = getWidth();
        int h = getHeight();
        int hw = w / 2;
        int hh = h / 2;
        int lf_w = loginForm.getWidth();
        int lf_h = loginForm.getHeight();
        int lf_x;
        int lf_y;
        int n_x;
        if (sidePanel == null || sidePanelComp == null) {
            switch (lfDirection) {
                case TOP_LEFT:
                case CENTER_LEFT:
                case BOTTOM_LEFT:
                    lf_x = margin;
                    break;
                case TOP:
                case CENTER:
                case BOTTOM:
                    lf_x = hw - lf_w / 2;
                    break;
                case TOP_RIGHT:
                case CENTER_RIGHT:
                case BOTTOM_RIGHT:
                    lf_x = w - lf_w - margin;
                    break;
                default:
                    throw new RuntimeException("unknown direction:" + lfDirection);
            }

            switch (lfDirection) {
                case TOP_LEFT:
                case TOP:
                case TOP_RIGHT:
                    lf_y = margin;
                    break;
                case CENTER_LEFT:
                case CENTER:
                case CENTER_RIGHT:
                    lf_y = hh - lf_h / 2;
                    break;
                case BOTTOM_LEFT:
                case BOTTOM:
                case BOTTOM_RIGHT:
                    lf_y = h - margin - lf_h;
                    break;
                default:
                    throw new RuntimeException("unknown direction:" + lfDirection);
            }
        } else {
            n_x = sidePanelComp.getWidth();
            int n_y = sidePanelComp.getHeight();
            int bw = lf_w + n_x + spaceBetween;
            int hbw = bw / 2;
            int sp_x;
            int sp_y;
            if (w > bw) {
                switch (lfDirection) {
                    case TOP_LEFT:
                    case CENTER_LEFT:
                    case BOTTOM_LEFT:
                        lf_x = margin;
                        sp_x = lf_x + lf_w + spaceBetween;
                        break;
                    case TOP:
                    case CENTER:
                    case BOTTOM:
                        lf_x = hw - hbw;
                        sp_x = lf_x + lf_w + spaceBetween;
                        break;
                    case TOP_RIGHT:
                    case CENTER_RIGHT:
                    case BOTTOM_RIGHT:
                        lf_x = w - margin - lf_w;
                        sp_x = lf_x - spaceBetween - n_x;
                        break;
                    default:
                        throw new RuntimeException("unknown direction:" + lfDirection);
                }

                switch (lfDirection) {
                    case TOP_LEFT:
                    case TOP:
                    case TOP_RIGHT:
                        sp_y = margin;
                        lf_y = margin;
                        break;
                    case CENTER_LEFT:
                    case CENTER:
                    case CENTER_RIGHT:
                        lf_y = hh - lf_h / 2;
                        sp_y = hh - n_y / 2;
                        break;
                    case BOTTOM_LEFT:
                    case BOTTOM:
                    case BOTTOM_RIGHT:
                        lf_y = h - margin - lf_h;
                        sp_y = h - margin - n_y;
                        break;
                    default:
                        throw new RuntimeException("unknown direction:" + lfDirection);
                }
            } else {
                lf_x = w * 2;
                lf_y = 0;
                sp_x = hw - n_x / 2;
                sp_y = hh - n_y / 2;
            }
            sidePanelComp.setLocation(sp_x, sp_y);
        }
        loginForm.setLocation(lf_x, lf_y);

        // Position glass overlay behind login form
        int glassPadding = SwingUtil.magnify(12);
        glassOverlay.setBounds(
                lf_x - glassPadding,
                lf_y - glassPadding,
                lf_w + glassPadding * 2,
                lf_h + glassPadding * 2
        );

        if (noticePanel.isLoaded()) {
            noticePanel.get().onResize();
        }

        int sn_y;
        switch (lfDirection) {
            case TOP_LEFT:
            case TOP:
            case TOP_RIGHT:
                sn_y = getHeight() - notificationPanel.height;
                break;
            default:
                sn_y = 0;
        }
        notificationPanel.setBounds(0, sn_y, getWidth(), notificationPanel.height);

        // Position bell icon at top-right corner
        int bellSize = bellIconButton.getPreferredSize().width;
        int bellMargin = SwingUtil.magnify(12);
        bellIconButton.setBounds(
                w - bellSize - bellMargin,
                bellMargin,
                bellSize,
                bellSize
        );
    }

    public DefaultScene.SidePanel getSidePanel() {
        return sidePanel;
    }

    public boolean isNoticeSidePanelEnabled() {
        return getMainPane().getRootFrame().getConfiguration().getBoolean("gui.notices.enabled");
    }

    public void setNoticeSidePanelEnabled(boolean e) {
        getMainPane().getRootFrame().getConfiguration().set("gui.notices.enabled", e);
        setSidePanel(null);
    }

    public void setSidePanel(DefaultScene.SidePanel side) {
        java.util.List<Notice> noticeList = getMainPane().getRootFrame().getNotices().getForCurrentLocale();
        if (side == null && isNoticeSidePanelEnabled() && noticeList != null && noticeList.size() > 3) {
            side = SidePanel.NOTICES;
        }
        if (sidePanel != side) {
            boolean noSidePanel = side == null;
            if (sidePanelComp != null) {
                sidePanelComp.setVisible(false);
            }

            sidePanel = side;
            sidePanelComp = noSidePanel ? null : getSidePanelComp(side);
            if (!noSidePanel) {
                sidePanelComp.setVisible(true);
            }

            if (noticePanel.isLoaded()) {
                noticePanel.get().setVisible(noSidePanel);
            }
            /*if(noSidePanel) {
                noticePanel.panelShown();
            }*/
            //noticePanel.setVisible(noSidePanel);
            updateCoords();

            validate();
            repaint();

            if (sidePanelComp != null) {
                sidePanelComp.validate();
                sidePanelComp.repaint();
            }
        }
    }

    public void toggleSidePanel(DefaultScene.SidePanel side) {
        if (sidePanel == side) {
            side = null;
        }

        setSidePanel(side);
    }

    public ExtendedPanel getSidePanelComp(DefaultScene.SidePanel side) {
        if (side == null) {
            throw new NullPointerException("side");
        } else {
            switch (side) {
                case SETTINGS:
                    return settingsForm.get();
                case NOTICES:
                    return noticeSidePanel.get();
                default:
                    throw new RuntimeException("unknown side:" + side);
            }
        }
    }

    public Direction getLoginFormDirection() {
        return lfDirection;
    }

    /**
     * Called when the bell icon button is clicked.
     * Shows update information if available, otherwise triggers a check.
     */
    private void onBellClicked() {
        UpdateChecker checker = getMainPane().getRootFrame().getLauncher().getUpdateChecker();
        if (checker == null) {
            return;
        }
        if (checker.isUpdateAvailable() && checker.getLatestUpdate() != null) {
            UpdateChecker.LauncherUpdate update = checker.getLatestUpdate();

            String changelog = update.getChangelog();
            if (changelog != null && changelog.length() > 200) {
                changelog = changelog.substring(0, 200) + "...";
            }

            int result = JOptionPane.showConfirmDialog(
                    this,
                    Localizable.get("bell.update.dialog.body",
                            update.getVersion().toString(),
                            changelog != null ? changelog : ""),
                    Localizable.get("bell.update.dialog.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE
            );
            if (result == JOptionPane.YES_OPTION) {
                update.openReleasePage();
            }
        } else {
            bellIconButton.setHasUpdate(false);
            bellIconButton.setEnabled(false);
            checker.checkForUpdates().thenAcceptAsync(opt ->
                    SwingUtilities.invokeLater(() -> {
                        bellIconButton.setEnabled(true);
                        if (opt.isPresent()) {
                            bellIconButton.setHasUpdate(true);
                            bellIconButton.setUpdateCount(1);
                        } else {
                            JOptionPane.showMessageDialog(
                                    this,
                                    Localizable.get("bell.update.none"),
                                    Localizable.get("bell.update.title"),
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        }
                    })
            , SwingUtil.executor());
        }
    }

    /**
     * Gets the bell icon button so external code can wire update checks.
     */
    public BellIconButton getBellIconButton() {
        return bellIconButton;
    }

    public void updateDirection() {
        loadDirection();
        updateCoords();
    }

    private void loadDirection() {
        Configuration config = getMainPane().getRootFrame().getConfiguration();
        Direction loginFormDirection = config.getDirection("gui.direction.loginform");
        if (loginFormDirection == null) {
            loginFormDirection = Direction.CENTER;
        }

        lfDirection = loginFormDirection;
    }

    public enum SidePanel {
        SETTINGS, NOTICES;

        public final boolean requiresShow;

        SidePanel(boolean requiresShow) {
            this.requiresShow = requiresShow;
        }

        SidePanel() {
            this(false);
        }
    }
}
