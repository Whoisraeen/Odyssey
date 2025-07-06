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
        // Validate initial position
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            System.err.println("Warning: Invalid initial player position: (" + x + ", " + y + ", " + z + "). Using fallback.");
            x = 0;
            y = 72; // Safe height above sea level
            z = 0;
        }
        
        this.position = new Vector3f(x, y, z);
        this.velocity = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.inventory = new Inventory();
        
        System.out.println("Player initialized at position: " + this.position);
    }

    public void update(float deltaTime, VoxelEngine engine, InputManager inputManager) {
        handleMovement(deltaTime, engine, inputManager);
        applyGravity(deltaTime, engine);
        checkEnvironmentDamage(engine);
    }
    
    public void update(float deltaTime, VoxelEngine engine, InputManager inputManager, com.odyssey.audio.GameSoundManager gameSoundManager) {
        handleMovement(deltaTime, engine, inputManager, gameSoundManager);
        applyGravity(deltaTime, engine);
        checkEnvironmentDamage(engine);
    }

    private void handleMovement(float deltaTime, VoxelEngine engine, InputManager inputManager, com.odyssey.audio.GameSoundManager gameSoundManager) {
        boolean wasMoving = velocity.x != 0 || velocity.z != 0;
        
        handleMovement(deltaTime, engine, inputManager);
        
        // Play footstep sounds when moving and on ground
        boolean isMoving = velocity.x != 0 || velocity.z != 0;
        if (isMoving && onGround && !wasMoving) {
            // Player just started moving
            BlockType blockBelow = engine.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y - 0.1), (int)Math.floor(position.z));
            gameSoundManager.playFootstepSound(blockBelow, new org.joml.Vector3f(position.x, position.y, position.z));
        }
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

        // Reset horizontal velocity first
        velocity.x = 0;
        velocity.z = 0;

        if (move.lengthSquared() > 0) {
            move.normalize();
            
            // Validate normalized vector
            if (!isValidVector(move)) {
                System.err.println("Warning: Invalid move vector after normalization: " + move);
                return;
            }
            
            move.rotateY((float) Math.toRadians(rotation.y));
            move.mul(currentSpeed);
            
            // Validate final move vector
            if (!isValidVector(move)) {
                System.err.println("Warning: Invalid move vector after transformation: " + move);
                return;
            }
            
            // Set velocity instead of directly modifying position
            velocity.x = move.x;
            velocity.z = move.z;
            
            // Validate velocity components
            if (!Float.isFinite(velocity.x)) {
                System.err.println("Warning: Invalid velocity.x calculated: " + velocity.x);
                velocity.x = 0;
            }
            if (!Float.isFinite(velocity.z)) {
                System.err.println("Warning: Invalid velocity.z calculated: " + velocity.z);
                velocity.z = 0;
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
        // Validate deltaTime
        if (!Float.isFinite(deltaTime) || deltaTime < 0) {
            System.err.println("Warning: Invalid deltaTime in applyGravity: " + deltaTime);
            return;
        }
        
        // Apply gravity
        float newVelocityY = velocity.y + GRAVITY * deltaTime;
        if (Float.isFinite(newVelocityY)) {
            velocity.y = newVelocityY;
        } else {
            System.err.println("Warning: Invalid velocity.y calculated: " + newVelocityY);
            velocity.y = 0;
        }

        float dx = velocity.x * deltaTime;
        float dy = velocity.y * deltaTime;
        float dz = velocity.z * deltaTime;
        
        // Validate movement deltas
        if (!Float.isFinite(dx)) {
            System.err.println("Warning: Invalid dx calculated: " + dx + " (velocity.x=" + velocity.x + ", deltaTime=" + deltaTime + ")");
            dx = 0;
        }
        if (!Float.isFinite(dy)) {
            System.err.println("Warning: Invalid dy calculated: " + dy + " (velocity.y=" + velocity.y + ", deltaTime=" + deltaTime + ")");
            dy = 0;
        }
        if (!Float.isFinite(dz)) {
            System.err.println("Warning: Invalid dz calculated: " + dz + " (velocity.z=" + velocity.z + ", deltaTime=" + deltaTime + ")");
            dz = 0;
        }

        // Check for collisions and resolve them one axis at a time
        // Y-axis
        float newY = position.y + dy;
        if (Float.isFinite(newY)) {
            position.y = newY;
            if (checkCollision(engine, 0, dy, 0)) {
                position.y -= dy;
                if (velocity.y < 0) {
                    onGround = true;
                }
                velocity.y = 0;
            } else {
                onGround = false;
            }
        } else {
            System.err.println("Warning: Invalid Y position would result: " + newY);
        }

        // X-axis
        float newX = position.x + dx;
        if (Float.isFinite(newX)) {
            position.x = newX;
            if (checkCollision(engine, dx, 0, 0)) {
                position.x -= dx;
            }
        } else {
            System.err.println("Warning: Invalid X position would result: " + newX);
        }

        // Z-axis
        float newZ = position.z + dz;
        if (Float.isFinite(newZ)) {
            position.z = newZ;
            if (checkCollision(engine, 0, 0, dz)) {
                position.z -= dz;
            }
        } else {
            System.err.println("Warning: Invalid Z position would result: " + newZ);
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
        if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)) {
            this.position.set(x, y, z);
        } else {
            System.err.println("Warning: Attempted to set invalid player position: (" + x + ", " + y + ", " + z + ")");
        }
    }
    
    private boolean isValidVector(Vector3f vector) {
        return Float.isFinite(vector.x) && Float.isFinite(vector.y) && Float.isFinite(vector.z);
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