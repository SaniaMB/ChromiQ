package io.github.saniamb.chromiq.core.api;

import io.github.saniamb.chromiq.core.palette.PaletteManager;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.awt.image.BufferedImage;
import java.io.Serializable;

/**
 * Manages the state for a single user session.
 * A new instance of this class is created for each user's session.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserSessionManager implements Serializable {

    private static final long serialVersionUID = 1L;

    private PaletteManager paletteManager;
    private transient BufferedImage currentImage; // Marked transient as BufferedImage is not serializable

    public PaletteManager getPaletteManager() {
        return paletteManager;
    }

    public void setPaletteManager(PaletteManager paletteManager) {
        this.paletteManager = paletteManager;
    }

    public BufferedImage getCurrentImage() {
        return currentImage;
    }

    public void setCurrentImage(BufferedImage currentImage) {
        this.currentImage = currentImage;
    }

    /**
     * Checks if the current session has a palette manager initialized.
     * @return true if a palette manager exists, false otherwise.
     */
    public boolean hasActiveSession() {
        return this.paletteManager != null;
    }
}