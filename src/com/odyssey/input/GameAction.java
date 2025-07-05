package com.odyssey.input;

public enum GameAction {
    MOVE_FORWARD,
    MOVE_BACK,
    MOVE_LEFT,
    MOVE_RIGHT,
    JUMP,
    CROUCH,
    SPRINT,

    // Block Interaction
    BREAK_BLOCK,
    PLACE_BLOCK,

    // Ship Controls
    TURN_LEFT,
    TURN_RIGHT,

    // UI & Inventory
    TOGGLE_INVENTORY,
    PAUSE_GAME
}