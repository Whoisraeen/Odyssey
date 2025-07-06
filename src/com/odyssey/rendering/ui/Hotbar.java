package com.odyssey.rendering.ui;

import com.odyssey.player.Inventory;
import com.odyssey.inventory.ItemStack;
import com.odyssey.rendering.Texture;
import com.odyssey.ui.UIRenderer;
import com.odyssey.world.BlockType;
import org.joml.Vector3f;

import java.util.Map;

public class Hotbar {

    private final UIRenderer uiRenderer;
    private final int hotbarTexture;
    private final int selectionTexture;
    private final int terrainTexture;

    private static final int SLOT_SIZE = 48;
    private static final int HOTBAR_WIDTH = SLOT_SIZE * Inventory.HOTBAR_SIZE;
    private static final int ATLAS_SIZE = 256; // 16x16 blocks
    private static final int TEXTURE_SIZE = 16;


    public Hotbar(UIRenderer uiRenderer) {
        this.uiRenderer = uiRenderer;
        this.hotbarTexture = Texture.loadTexture("assets/textures/ui/hotbar.png");
        this.selectionTexture = Texture.loadTexture("assets/textures/ui/hotbar_selection.png");
        // Temporarily comment out terrain texture to isolate the issue
        this.terrainTexture = 0; // Texture.loadTexture("assets/textures/terrain.png");
    }

    public void render(int windowWidth, int windowHeight, Inventory inventory) {
        float hotbarX = (windowWidth / 2f) - (HOTBAR_WIDTH / 2f);
        float hotbarY = windowHeight - SLOT_SIZE - 10; // 10px from bottom

        // Draw hotbar background
        uiRenderer.drawRect(hotbarX, hotbarY, HOTBAR_WIDTH, SLOT_SIZE, 0xFF404040); // Gray background

        // Draw items
        for (Map.Entry<Integer, ItemStack> entry : inventory.getHotbarItems().entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            float itemX = hotbarX + slot * SLOT_SIZE + (SLOT_SIZE / 2f) - (32 / 2f); // Center item in slot
            float itemY = hotbarY + (SLOT_SIZE / 2f) - (32 / 2f);
            
            // This is a placeholder for a real block icon rendering system
            // It would need to look up UVs from the block type
            // For now, we just draw a stone texture for everything
            // Temporarily commented out: uiRenderer.draw(terrainTexture, itemX, itemY, 32, 32, 0, new Vector3f(1, 1, 1));
        }

        // Draw selection
        int selectedSlot = inventory.getSelectedSlot();
        float selectionX = hotbarX + selectedSlot * SLOT_SIZE;
        uiRenderer.drawRect(selectionX, hotbarY, SLOT_SIZE, SLOT_SIZE, 0xFFFFFFFF); // White selection highlight
    }
}