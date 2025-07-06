package com.odyssey.rendering.ui;

import com.odyssey.rendering.Texture;
import com.odyssey.ui.UIRenderer;
import org.joml.Vector3f;

public class Crosshair {

    private final UIRenderer uiRenderer;
    private final int crosshairTexture;
    private static final int CROSSHAIR_SIZE = 24;

    public Crosshair(UIRenderer uiRenderer) {
        this.uiRenderer = uiRenderer;
        this.crosshairTexture = Texture.loadTexture("assets/textures/ui/crosshair.png");
    }

    public void render(int windowWidth, int windowHeight) {
        float x = (windowWidth / 2f) - (CROSSHAIR_SIZE / 2f);
        float y = (windowHeight / 2f) - (CROSSHAIR_SIZE / 2f);
        uiRenderer.drawRect(x, y, CROSSHAIR_SIZE, CROSSHAIR_SIZE, 0xFFFFFFFF); // White crosshair
    }

    public void cleanup() {
        // Texture cleanup should be managed by a larger asset manager,
        // but for now we can do it here if needed.
    }
}