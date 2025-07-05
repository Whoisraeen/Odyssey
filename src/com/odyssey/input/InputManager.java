package com.odyssey.input;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import org.joml.Vector2f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {

    private final long window;

    private final Map<GameAction, Integer> keyboardMappings = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Integer> mouseMappings = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Integer> controllerMappings = new EnumMap<>(GameAction.class);

    private final Map<GameAction, Boolean> previousActionStates = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Boolean> currentActionStates = new EnumMap<>(GameAction.class);
    
    private boolean controllerConnected = false;

    public InputManager(long window) {
        this.window = window;
        initializeDefaultMappings();
        
        // Initialize action states
        for (GameAction action : GameAction.values()) {
            previousActionStates.put(action, false);
            currentActionStates.put(action, false);
        }
    }

    private void initializeDefaultMappings() {
        // Keyboard
        keyboardMappings.put(GameAction.MOVE_FORWARD, GLFW_KEY_W);
        keyboardMappings.put(GameAction.MOVE_BACK, GLFW_KEY_S);
        keyboardMappings.put(GameAction.MOVE_LEFT, GLFW_KEY_A);
        keyboardMappings.put(GameAction.MOVE_RIGHT, GLFW_KEY_D);
        keyboardMappings.put(GameAction.JUMP, GLFW_KEY_SPACE);
        keyboardMappings.put(GameAction.SPRINT, GLFW_KEY_LEFT_SHIFT);
        keyboardMappings.put(GameAction.CROUCH, GLFW_KEY_LEFT_CONTROL);
        keyboardMappings.put(GameAction.PAUSE_GAME, GLFW_KEY_ESCAPE);

        // Mouse
        mouseMappings.put(GameAction.BREAK_BLOCK, GLFW_MOUSE_BUTTON_LEFT);
        mouseMappings.put(GameAction.PLACE_BLOCK, GLFW_MOUSE_BUTTON_RIGHT);

        // Controller (Standard Gamepad Layout)
        controllerMappings.put(GameAction.JUMP, GLFW_GAMEPAD_BUTTON_A);
        controllerMappings.put(GameAction.BREAK_BLOCK, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER);
        controllerMappings.put(GameAction.PLACE_BLOCK, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER);
        controllerMappings.put(GameAction.SPRINT, GLFW_GAMEPAD_BUTTON_LEFT_THUMB);
        controllerMappings.put(GameAction.PAUSE_GAME, GLFW_GAMEPAD_BUTTON_START);
    }
    
    public void update() {
        // Update previous state
        previousActionStates.putAll(currentActionStates);

        // Poll controller
        controllerConnected = glfwJoystickIsGamepad(GLFW_JOYSTICK_1);
        
        GLFWGamepadState gamepadState = null;
        if (controllerConnected) {
             gamepadState = GLFWGamepadState.create();
             glfwGetGamepadState(GLFW_JOYSTICK_1, gamepadState);
        }

        // Update current state for all actions
        for (GameAction action : GameAction.values()) {
            boolean isPressed = checkAction(action, gamepadState);
            currentActionStates.put(action, isPressed);
        }
    }

    private boolean checkAction(GameAction action, GLFWGamepadState gamepadState) {
        // Check keyboard
        if (keyboardMappings.containsKey(action)) {
            if (glfwGetKey(window, keyboardMappings.get(action)) == GLFW_PRESS) {
                return true;
            }
        }
        // Check mouse
        if (mouseMappings.containsKey(action)) {
            if (glfwGetMouseButton(window, mouseMappings.get(action)) == GLFW_PRESS) {
                return true;
            }
        }
        // Check controller
        if (controllerConnected && controllerMappings.containsKey(action)) {
             if (gamepadState.buttons(controllerMappings.get(action)) == GLFW_PRESS) {
                 return true;
             }
        }
        return false;
    }

    public boolean isActionPressed(GameAction action) {
        return currentActionStates.getOrDefault(action, false);
    }
    
    public boolean isActionJustPressed(GameAction action) {
        boolean previouslyPressed = previousActionStates.getOrDefault(action, false);
        boolean currentlyPressed = currentActionStates.getOrDefault(action, false);
        return currentlyPressed && !previouslyPressed;
    }
    
    public Vector2f getMovementAxes() {
        if(controllerConnected) {
            GLFWGamepadState gamepadState = GLFWGamepadState.create();
            if (glfwGetGamepadState(GLFW_JOYSTICK_1, gamepadState)) {
                float x = gamepadState.axes(GLFW_GAMEPAD_AXIS_LEFT_X);
                float y = gamepadState.axes(GLFW_GAMEPAD_AXIS_LEFT_Y);
                // Apply deadzone
                if (Math.abs(x) < 0.1f) x = 0;
                if (Math.abs(y) < 0.1f) y = 0;
                return new Vector2f(x, -y); // Y is often inverted
            }
        }
        
        // Fallback to keyboard
        float x = 0, y = 0;
        if (isActionPressed(GameAction.MOVE_RIGHT)) x += 1;
        if (isActionPressed(GameAction.MOVE_LEFT)) x -= 1;
        if (isActionPressed(GameAction.MOVE_FORWARD)) y += 1;
        if (isActionPressed(GameAction.MOVE_BACK)) y -= 1;
        return new Vector2f(x, y).normalize();
    }
    
    public double getMouseX() {
        double[] xpos = new double[1];
        glfwGetCursorPos(window, xpos, null);
        return xpos[0];
    }
    
    public double getMouseY() {
        double[] ypos = new double[1];
        glfwGetCursorPos(window, null, ypos);
        return ypos[0];
    }
}