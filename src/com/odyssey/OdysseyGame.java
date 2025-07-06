package com.odyssey;

import com.odyssey.core.VoxelEngine;
import com.odyssey.entity.Ship;
import com.odyssey.input.GameAction;
import com.odyssey.input.InputManager;
import org.joml.Vector2f;
import com.odyssey.player.Player;
import com.odyssey.rendering.Camera;
import com.odyssey.rendering.ui.Hotbar;
import com.odyssey.rendering.ui.Crosshair;
import com.odyssey.rendering.ui.FontManager;
import com.odyssey.rendering.ui.TextRenderer;

import com.odyssey.ui.UIRenderer;
import com.odyssey.ui.GameState;
import com.odyssey.ui.GameStateManager;
import org.joml.Vector3f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_INVALID_ENUM;
import static org.lwjgl.opengl.GL11.GL_INVALID_VALUE;
import static org.lwjgl.opengl.GL11.GL_INVALID_OPERATION;
import static org.lwjgl.opengl.GL11.GL_OUT_OF_MEMORY;

import com.odyssey.audio.SoundManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@SpringBootApplication
@Component
public class OdysseyGame implements Runnable {
    private long window;
    private int width = 1280;
    private int height = 720;
    private GameStateManager gameStateManager;
    
    @Autowired
    private VoxelEngine voxelEngine;
    
    private Camera camera;
    private double lastFrameTime;
    private InputManager inputManager;
    private UIRenderer uiRenderer;
    private Crosshair crosshair;
    private Hotbar hotbar;
    private SoundManager soundManager;
    
    private static ApplicationContext applicationContext;
    
    // Mouse input tracking
    private boolean leftMousePressed = false;
    private double mouseX, mouseY;
    private double lastMouseX, lastMouseY;
    private boolean firstMouse = true;
    private final float mouseSensitivity = 0.1f;

    public static void main(String[] args) {
        // Start Spring Boot application
        applicationContext = SpringApplication.run(OdysseyGame.class, args);
        
        // Get the OdysseyGame bean and run it
        OdysseyGame game = applicationContext.getBean(OdysseyGame.class);
        game.run();
    }

    @Override
    public void run() {
        try {
            init();
            gameLoop();
        } finally {
            cleanup();
            if (soundManager != null) {
                soundManager.cleanup();
            }
            if (voxelEngine != null) {
                voxelEngine.cleanup();
            }
        }
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Create the window
        window = glfwCreateWindow(width, height, "Odyssey", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (gameStateManager != null) {
                if (gameStateManager.getCurrentState() == GameState.IN_GAME) {
                    if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                        gameStateManager.setState(GameState.PAUSE_MENU);
                        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                    }
                } else {
                    gameStateManager.handleKeyInput(key, action);
                }
            } else {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                    glfwSetWindowShouldClose(window, true);
                }
            }
        });
        
        // Setup mouse button callback
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                leftMousePressed = (action == GLFW_PRESS);
                if (gameStateManager != null && gameStateManager.getCurrentState() != GameState.IN_GAME) {
                    gameStateManager.handleMouseInput(mouseX, mouseY, leftMousePressed);
                }
            }
        });
        
        // Setup cursor position callback
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
            if (gameStateManager != null) {
                if (gameStateManager.getCurrentState() == GameState.IN_GAME) {
                    handleMouseMovement(xpos, ypos);
                } else {
                    gameStateManager.handleMouseInput(mouseX, mouseY, false);
                }
            }
        });
        
        glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
            if (voxelEngine != null && voxelEngine.getPlayer() != null) {
                voxelEngine.getPlayer().handleScroll(yoffset);
            }
        });

        // Start with cursor enabled for menu
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
            
            // Update UI renderer projection matrix
            if (uiRenderer != null) {
                uiRenderer.updateScreenSize(w, h);
            }
            
            if (voxelEngine != null && h > 0) {
                float aspectRatio = (float) w / h;
                // Validate aspect ratio before setting
                if (Float.isFinite(aspectRatio) && aspectRatio > 0.1f && aspectRatio < 10.0f) {
                    voxelEngine.getCamera().setAspectRatio(aspectRatio);
                } else {
                    System.err.println("Warning: Invalid aspect ratio calculated: " + aspectRatio + " (w=" + w + ", h=" + h + ")");
                }
            }
        });
        
        // Mouse cursor position callback is already set above

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        
        // Set initial viewport - critical for proper rendering
        glViewport(0, 0, width, height);
        System.out.println("DEBUG: Initial viewport set to " + width + "x" + height);
        
        // Start with cursor enabled for main menu
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        // Enable OpenGL features
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        
        soundManager = new SoundManager();
        soundManager.init();

        // Initialize UI renderer
        try {
            System.out.println("DEBUG: Creating UIRenderer...");
            FontManager fontManager = FontManager.getInstance();
            TextRenderer textRenderer = fontManager.createTextRenderer(width, height);
            uiRenderer = new UIRenderer(textRenderer);
            System.out.println("DEBUG: UIRenderer created successfully");
        } catch (Exception e) {
            System.err.println("ERROR: Failed to initialize UI renderer: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        gameStateManager = new GameStateManager(uiRenderer, soundManager, width, height);
        System.out.println("DEBUG: GameStateManager created successfully");
        
        // Initialize game components that will be used when entering game state
        crosshair = new Crosshair(uiRenderer);
        System.out.println("DEBUG: Crosshair created successfully");
        
        hotbar = new Hotbar(uiRenderer);
        System.out.println("DEBUG: Hotbar created successfully");
        
        lastFrameTime = glfwGetTime();
        System.out.println("DEBUG: Last frame time set");

        inputManager = new InputManager(window);
        System.out.println("DEBUG: InputManager created successfully");
        
        System.out.println("DEBUG: Initialization complete, starting game loop");
        System.out.println("DEBUG: Current GameState: " + gameStateManager.getCurrentState());
        System.out.println("DEBUG: Window should close: " + glfwWindowShouldClose(window));
    }

    private void gameLoop() {
        while (!glfwWindowShouldClose(window)) {
            double currentTime = glfwGetTime();
            float deltaTime = (float)(currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            inputManager.update();
            
            // Handle state transitions
            GameState currentState = gameStateManager.getCurrentState();
            
            if (currentState == GameState.IN_GAME) {
                // Initialize VoxelEngine if entering game for first time
                if (camera == null) {
                    voxelEngine.initialize(width, height, soundManager);
                    camera = voxelEngine.getCamera();
                    gameStateManager.setVoxelEngine(voxelEngine);
                }
                
                // Ensure cursor is disabled for game
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
                
                // Set game clear color
                glClearColor(0.5f, 0.8f, 1.0f, 1.0f); // Sky blue
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                
                handleInput(deltaTime);
                voxelEngine.update(deltaTime, inputManager);
                
                voxelEngine.render();

                // Render game UI
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                
                int[] windowWidth = new int[1];
                int[] windowHeight = new int[1];
                glfwGetWindowSize(window, windowWidth, windowHeight);

                crosshair.render(windowWidth[0], windowHeight[0]);
                hotbar.render(windowWidth[0], windowHeight[0], voxelEngine.getPlayer().getInventory());
                
                // Draw health
                String healthText = "Health: " + (int)voxelEngine.getPlayer().getHealth();
                uiRenderer.drawText(healthText, 20.0f, 20.0f, 1.0f, new Vector3f(1.0f, 1.0f, 1.0f)); // White color
                
                glDisable(GL_BLEND);
                glEnable(GL_DEPTH_TEST);
            } else if (currentState == GameState.PAUSE_MENU) {
                // Render game in background (paused)
                if (voxelEngine != null) {
                    glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
                    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                    voxelEngine.render(); // Render without updating
                }
                
                // Render pause menu overlay
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                
                int[] windowWidth = new int[1];
                int[] windowHeight = new int[1];
                glfwGetWindowSize(window, windowWidth, windowHeight);
                
                gameStateManager.render(windowWidth[0], windowHeight[0]);
                
                glDisable(GL_BLEND);
                glEnable(GL_DEPTH_TEST);
            } else {
                // Menu states
                glClearColor(0.1f, 0.1f, 0.2f, 1.0f); // Dark background for menus
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                
                glDisable(GL_DEPTH_TEST);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                
                int[] windowWidth = new int[1];
                int[] windowHeight = new int[1];
                glfwGetWindowSize(window, windowWidth, windowHeight);
                
                gameStateManager.render(windowWidth[0], windowHeight[0]);
                
                glDisable(GL_BLEND);
                glEnable(GL_DEPTH_TEST);
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void handleMouseMovement(double xpos, double ypos) {
        if (firstMouse) {
            lastMouseX = xpos;
            lastMouseY = ypos;
            firstMouse = false;
        }
        
        double xoffset = xpos - lastMouseX;
        double yoffset = lastMouseY - ypos; // Reversed since y-coordinates go from bottom to top
        lastMouseX = xpos;
        lastMouseY = ypos;
        
        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;
        
        if (camera != null) {
            camera.rotate((float)xoffset, (float)yoffset);
        }
    }
    
    private void handleInput(float deltaTime) {
        // Mouse input is handled by InputManager and VoxelEngine

        Player player = voxelEngine.getPlayer();
        
        if (player.isControllingShip()) {
            // Ship controls
            Ship ship = player.getControlledShip();
            boolean forward = inputManager.isActionPressed(GameAction.MOVE_FORWARD);
            boolean back = inputManager.isActionPressed(GameAction.MOVE_BACK);
            boolean left = inputManager.isActionPressed(GameAction.MOVE_LEFT);
            boolean right = inputManager.isActionPressed(GameAction.MOVE_RIGHT);
            ship.applyInput(forward, back, left, right);
        } else {
            // Player controls
            Vector2f movement = inputManager.getMovementAxes();
            
            Vector3f forward = camera.getFront();
            Vector3f right = camera.getRight();

            Vector3f moveDir = new Vector3f(0, 0, 0);
            moveDir.add(new Vector3f(forward.x, 0, forward.z).normalize().mul(movement.y));
            moveDir.add(new Vector3f(right.x, 0, right.z).normalize().mul(movement.x));

            if (moveDir.lengthSquared() > 0) {
                moveDir.normalize();
            }

            player.getVelocity().x += moveDir.x * Player.MOVE_SPEED;
            player.getVelocity().z += moveDir.z * Player.MOVE_SPEED;
            
            if (inputManager.isActionJustPressed(GameAction.JUMP)) {
                player.jump();
            }
        }
    }

    private void cleanup() {
        if (voxelEngine != null) {
            voxelEngine.cleanup();
        }
        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}