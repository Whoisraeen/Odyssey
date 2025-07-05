package com.odyssey.player;

import com.odyssey.core.VoxelEngine;
import com.odyssey.entity.Ship;
import com.odyssey.input.GameAction;
import com.odyssey.input.InputManager;
import com.odyssey.world.BlockType;
import com.odyssey.world.SpawnFinder;
import org.joml.Vector3f;

public class Player {

    private static final float GRAVITY = -30.0f;
    public static final float MOVE_SPEED = 5.0f;
    public static final float JUMP_POWER = 10.0f;

    private final Vector3f position;
    private final Vector3f velocity;
    private final Vector3f rotation;
    private final Inventory inventory;
    
    private Ship controlledShip = null;

    private boolean onGround = false;

    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float PLAYER_WIDTH = 0.6f;

    private float speed = 5.0f;
    private float jumpHeight = 10.0f;

    private float health = 20.0f;
    private float maxHealth = 20.0f;

    public Player(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
        this.velocity = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.inventory = new Inventory();
    }

    public void update(float deltaTime, VoxelEngine engine, InputManager inputManager) {
        handleMovement(deltaTime, engine, inputManager);
        applyGravity(deltaTime, engine);
        checkEnvironmentDamage(engine);
    }

    private void handleMovement(float deltaTime, VoxelEngine engine, InputManager inputManager) {
        float currentSpeed = speed;

        // Check for slowdown from snow
        BlockType blockBelow = engine.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y - 0.1), (int)Math.floor(position.z));
        if (blockBelow == BlockType.SNOW_LAYER) {
            currentSpeed *= 0.8f; // 20% slowdown on snow
        }

        Vector3f move = new Vector3f();
        if (inputManager.isActionPressed(GameAction.MOVE_FORWARD)) {
            move.z -= 1;
        }
        if (inputManager.isActionPressed(GameAction.MOVE_BACK)) {
            move.z += 1;
        }
        if (inputManager.isActionPressed(GameAction.MOVE_LEFT)) {
            move.x -= 1;
        }
        if (inputManager.isActionPressed(GameAction.MOVE_RIGHT)) {
            move.x += 1;
        }

        if (move.lengthSquared() > 0) {
            move.normalize();
            move.rotateY((float) Math.toRadians(rotation.y));
            move.mul(currentSpeed * deltaTime);
            
            // Basic collision detection
            if (isPositionValid(new Vector3f(position).add(move.x, 0, 0), engine)) {
                position.x += move.x;
            }
            if (isPositionValid(new Vector3f(position).add(0, move.y, 0), engine)) {
                position.y += move.y;
            }
            if (isPositionValid(new Vector3f(position).add(0, 0, move.z), engine)) {
                position.z += move.z;
            }
        }
        
        if (inputManager.isActionPressed(GameAction.JUMP) && onGround) {
            velocity.y = jumpHeight;
            onGround = false;
        }
    }

    private boolean checkCollision(VoxelEngine world, float dx, float dy, float dz) {
        // Create an AABB for the player
        Vector3f min = new Vector3f(position.x - PLAYER_WIDTH / 2, position.y, position.z - PLAYER_WIDTH / 2);
        Vector3f max = new Vector3f(position.x + PLAYER_WIDTH / 2, position.y + PLAYER_HEIGHT, position.z + PLAYER_WIDTH / 2);

        // Check all blocks that could be colliding with the player AABB
        for (int x = (int) Math.floor(min.x); x <= Math.floor(max.x); x++) {
            for (int y = (int) Math.floor(min.y); y <= Math.floor(max.y); y++) {
                for (int z = (int) Math.floor(min.z); z <= Math.floor(max.z); z++) {
                    if (world.getBlock(x, y, z) != BlockType.AIR) {
                        return true; // Collision detected
                    }
                }
            }
        }
        return false;
    }

    private void applyGravity(float deltaTime, VoxelEngine engine) {
        // Apply gravity
        velocity.y += GRAVITY * deltaTime;

        float dx = velocity.x * deltaTime;
        float dy = velocity.y * deltaTime;
        float dz = velocity.z * deltaTime;

        // Reset horizontal velocity after applying it for this frame
        velocity.x = 0;
        velocity.z = 0;

        // Broad-phase collision check extents
        float checkWidth = PLAYER_WIDTH / 2f;

        // Check for collisions and resolve them one axis at a time
        // Y-axis
        position.y += dy;
        if (checkCollision(engine, 0, dy, 0)) {
            position.y -= dy;
            if (velocity.y < 0) {
                onGround = true;
            }
            velocity.y = 0;
        } else {
            onGround = false;
        }

        // X-axis
        position.x += dx;
        if (checkCollision(engine, dx, 0, 0)) {
            position.x -= dx;
        }

        // Z-axis
        position.z += dz;
        if (checkCollision(engine, 0, 0, dz)) {
            position.z -= dz;
        }
    }

    private boolean isPositionValid(Vector3f newPosition, VoxelEngine engine) {
        // Create an AABB for the player
        Vector3f min = new Vector3f(newPosition.x - PLAYER_WIDTH / 2, newPosition.y, newPosition.z - PLAYER_WIDTH / 2);
        Vector3f max = new Vector3f(newPosition.x + PLAYER_WIDTH / 2, newPosition.y + PLAYER_HEIGHT, newPosition.z + PLAYER_WIDTH / 2);

        // Check all blocks that could be colliding with the player AABB
        for (int x = (int) Math.floor(min.x); x <= Math.floor(max.x); x++) {
            for (int y = (int) Math.floor(min.y); y <= Math.floor(max.y); y++) {
                for (int z = (int) Math.floor(min.z); z <= Math.floor(max.z); z++) {
                    if (engine.getBlock(x, y, z) != BlockType.AIR) {
                        return false; // Collision detected
                    }
                }
            }
        }
        return true;
    }

    public void jump() {
        if (onGround) {
            velocity.y = JUMP_POWER;
            onGround = false;
        }
    }

    public void handleScroll(double yoffset) {
        int currentSlot = inventory.getSelectedSlot();
        if (yoffset > 0) {
            // Scroll up
            currentSlot--;
        } else {
            // Scroll down
            currentSlot++;
        }

        if (currentSlot < 0) {
            currentSlot = Inventory.HOTBAR_SIZE - 1;
        } else if (currentSlot >= Inventory.HOTBAR_SIZE) {
            currentSlot = 0;
        }
        inventory.setSelectedSlot(currentSlot);
    }

    public boolean isCollidingWith(int x, int y, int z) {
        // Player's AABB
        Vector3f min = new Vector3f(position.x - PLAYER_WIDTH / 2, position.y, position.z - PLAYER_WIDTH / 2);
        Vector3f max = new Vector3f(position.x + PLAYER_WIDTH / 2, position.y + PLAYER_HEIGHT, position.z + PLAYER_WIDTH / 2);

        // Block's AABB
        Vector3f blockMin = new Vector3f(x, y, z);
        Vector3f blockMax = new Vector3f(x + 1, y + 1, z + 1);

        // Check for overlap
        return (min.x < blockMax.x && max.x > blockMin.x) &&
               (min.y < blockMax.y && max.y > blockMin.y) &&
               (min.z < blockMax.z && max.z > blockMin.z);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }
    
    public Vector3f getRotation() {
        return rotation;
    }
    
    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
    
    public void setControlledShip(Ship ship) {
        this.controlledShip = ship;
    }
    
    public Ship getControlledShip() {
        return controlledShip;
    }
    
    public boolean isControllingShip() {
        return controlledShip != null;
    }

    private void checkEnvironmentDamage(VoxelEngine engine) {
        BlockType standingIn = engine.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y), (int)Math.floor(position.z));
        if (standingIn == BlockType.FIRE) {
            takeDamage(1.0f, engine); // Damage per second in fire
        }
    }
    
    public void takeDamage(float amount, VoxelEngine engine) {
        this.health -= amount;
        System.out.println("Player took " + amount + " damage. Health is now " + this.health);
        if (this.health <= 0) {
            die(engine);
        }
    }
    
    private void die(VoxelEngine engine) {
        System.out.println("Player has died!");
        this.health = this.maxHealth;
        // Find a safe respawn location
        Vector3f respawnLocation = SpawnFinder.findSafeSpawnLocation(engine.getWorldGenerator());
        this.position.set(respawnLocation.x, respawnLocation.y, respawnLocation.z);
        this.velocity.zero();
    }
    
    public float getHealth() {
        return health;
    }
    
    public float getMaxHealth() {
        return maxHealth;
    }
}