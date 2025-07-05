package com.odyssey.ui.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * RenderContext for GPU state management and rendering operations
 * Provides centralized state management and optimization
 */
public class RenderContext {
    private static final String TAG = "RenderContext";
    
    // Singleton instance
    private static RenderContext instance;
    private static final Object lock = new Object();
    
    // State tracking
    private final RenderState currentState;
    private final Stack<RenderState> stateStack;
    private final Map<String, Object> properties;
    
    // Resource tracking
    private int boundShaderProgram;
    private int boundVertexArray;
    private int[] boundTextures;
    private int boundFramebuffer;
    private int activeTextureUnit;
    
    // Statistics
    private long frameCount;
    private long drawCalls;
    private long stateChanges;
    private long textureBinds;
    private long shaderBinds;
    
    /**
     * Render state container
     */
    public static class RenderState {
        // Blend state
        public boolean blendEnabled = false;
        public int blendSrcFactor = GL11.GL_SRC_ALPHA;
        public int blendDstFactor = GL11.GL_ONE_MINUS_SRC_ALPHA;
        
        // Depth state
        public boolean depthTestEnabled = false;
        public boolean depthWriteEnabled = true;
        public int depthFunc = GL11.GL_LESS;
        
        // Stencil state
        public boolean stencilTestEnabled = false;
        public int stencilFunc = GL11.GL_ALWAYS;
        public int stencilRef = 0;
        public int stencilMask = 0xFF;
        public int stencilFail = GL11.GL_KEEP;
        public int stencilZFail = GL11.GL_KEEP;
        public int stencilZPass = GL11.GL_KEEP;
        
        // Cull state
        public boolean cullFaceEnabled = false;
        public int cullFaceMode = GL11.GL_BACK;
        public int frontFace = GL11.GL_CCW;
        
        // Scissor state
        public boolean scissorTestEnabled = false;
        public int scissorX = 0;
        public int scissorY = 0;
        public int scissorWidth = 0;
        public int scissorHeight = 0;
        
        // Viewport state
        public int viewportX = 0;
        public int viewportY = 0;
        public int viewportWidth = 800;
        public int viewportHeight = 600;
        
        // Clear color
        public float clearR = 0.0f;
        public float clearG = 0.0f;
        public float clearB = 0.0f;
        public float clearA = 1.0f;
        
        /**
         * Copy constructor
         */
        public RenderState(RenderState other) {
            if (other != null) {
                this.blendEnabled = other.blendEnabled;
                this.blendSrcFactor = other.blendSrcFactor;
                this.blendDstFactor = other.blendDstFactor;
                
                this.depthTestEnabled = other.depthTestEnabled;
                this.depthWriteEnabled = other.depthWriteEnabled;
                this.depthFunc = other.depthFunc;
                
                this.stencilTestEnabled = other.stencilTestEnabled;
                this.stencilFunc = other.stencilFunc;
                this.stencilRef = other.stencilRef;
                this.stencilMask = other.stencilMask;
                this.stencilFail = other.stencilFail;
                this.stencilZFail = other.stencilZFail;
                this.stencilZPass = other.stencilZPass;
                
                this.cullFaceEnabled = other.cullFaceEnabled;
                this.cullFaceMode = other.cullFaceMode;
                this.frontFace = other.frontFace;
                
                this.scissorTestEnabled = other.scissorTestEnabled;
                this.scissorX = other.scissorX;
                this.scissorY = other.scissorY;
                this.scissorWidth = other.scissorWidth;
                this.scissorHeight = other.scissorHeight;
                
                this.viewportX = other.viewportX;
                this.viewportY = other.viewportY;
                this.viewportWidth = other.viewportWidth;
                this.viewportHeight = other.viewportHeight;
                
                this.clearR = other.clearR;
                this.clearG = other.clearG;
                this.clearB = other.clearB;
                this.clearA = other.clearA;
            }
        }
        
        /**
         * Default constructor
         */
        public RenderState() {
            // Use default values
        }
    }
    
    /**
     * Private constructor
     */
    private RenderContext() {
        this.currentState = new RenderState();
        this.stateStack = new Stack<>();
        this.properties = new HashMap<>();
        this.boundTextures = new int[32]; // Support up to 32 texture units
        
        // Initialize state
        reset();
    }
    
    /**
     * Get the singleton instance
     */
    public static RenderContext getInstance() {
        synchronized (lock) {
            if (instance == null) {
                instance = new RenderContext();
            }
            return instance;
        }
    }
    
    /**
     * Initialize the render context
     */
    public void initialize() {
        System.out.println("RenderContext initializing...");
        
        // Query OpenGL capabilities
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        String version = GL11.glGetString(GL11.GL_VERSION);
        String glslVersion = GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION);
        
        System.out.println("OpenGL Vendor: " + vendor);
        System.out.println("OpenGL Renderer: " + renderer);
        System.out.println("OpenGL Version: " + version);
        System.out.println("GLSL Version: " + glslVersion);
        
        // Store capabilities
        properties.put("vendor", vendor);
        properties.put("renderer", renderer);
        properties.put("version", version);
        properties.put("glslVersion", glslVersion);
        
        // Query limits
        int maxTextureUnits = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        int maxViewportDims[] = new int[2];
        GL11.glGetIntegerv(GL11.GL_MAX_VIEWPORT_DIMS, maxViewportDims);
        
        properties.put("maxTextureUnits", maxTextureUnits);
        properties.put("maxTextureSize", maxTextureSize);
        properties.put("maxViewportWidth", maxViewportDims[0]);
        properties.put("maxViewportHeight", maxViewportDims[1]);
        
        System.out.println("Max Texture Units: " + maxTextureUnits);
        System.out.println("Max Texture Size: " + maxTextureSize);
        System.out.println("Max Viewport: " + maxViewportDims[0] + "x" + maxViewportDims[1]);
        
        // Apply initial state
        applyState();
        
        System.out.println("RenderContext initialized");
    }
    
    /**
     * Reset the render context to default state
     */
    public void reset() {
        // Clear state stack
        stateStack.clear();
        
        // Reset resource bindings
        boundShaderProgram = 0;
        boundVertexArray = 0;
        boundFramebuffer = 0;
        activeTextureUnit = 0;
        
        for (int i = 0; i < boundTextures.length; i++) {
            boundTextures[i] = 0;
        }
        
        // Reset statistics
        frameCount = 0;
        drawCalls = 0;
        stateChanges = 0;
        textureBinds = 0;
        shaderBinds = 0;
    }
    
    /**
     * Push current state onto the stack
     */
    public void pushState() {
        stateStack.push(new RenderState(currentState));
    }
    
    /**
     * Pop state from the stack and apply it
     */
    public void popState() {
        if (!stateStack.isEmpty()) {
            RenderState restoredState = stateStack.pop();
            copyState(restoredState, currentState);
            applyState();
        }
    }
    
    /**
     * Copy state from source to destination
     */
    private void copyState(RenderState src, RenderState dst) {
        dst.blendEnabled = src.blendEnabled;
        dst.blendSrcFactor = src.blendSrcFactor;
        dst.blendDstFactor = src.blendDstFactor;
        
        dst.depthTestEnabled = src.depthTestEnabled;
        dst.depthWriteEnabled = src.depthWriteEnabled;
        dst.depthFunc = src.depthFunc;
        
        dst.stencilTestEnabled = src.stencilTestEnabled;
        dst.stencilFunc = src.stencilFunc;
        dst.stencilRef = src.stencilRef;
        dst.stencilMask = src.stencilMask;
        dst.stencilFail = src.stencilFail;
        dst.stencilZFail = src.stencilZFail;
        dst.stencilZPass = src.stencilZPass;
        
        dst.cullFaceEnabled = src.cullFaceEnabled;
        dst.cullFaceMode = src.cullFaceMode;
        dst.frontFace = src.frontFace;
        
        dst.scissorTestEnabled = src.scissorTestEnabled;
        dst.scissorX = src.scissorX;
        dst.scissorY = src.scissorY;
        dst.scissorWidth = src.scissorWidth;
        dst.scissorHeight = src.scissorHeight;
        
        dst.viewportX = src.viewportX;
        dst.viewportY = src.viewportY;
        dst.viewportWidth = src.viewportWidth;
        dst.viewportHeight = src.viewportHeight;
        
        dst.clearR = src.clearR;
        dst.clearG = src.clearG;
        dst.clearB = src.clearB;
        dst.clearA = src.clearA;
    }
    
    /**
     * Apply current state to OpenGL
     */
    public void applyState() {
        // Apply blend state
        if (currentState.blendEnabled) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(currentState.blendSrcFactor, currentState.blendDstFactor);
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
        
        // Apply depth state
        if (currentState.depthTestEnabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(currentState.depthFunc);
        } else {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthMask(currentState.depthWriteEnabled);
        
        // Apply stencil state
        if (currentState.stencilTestEnabled) {
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glStencilFunc(currentState.stencilFunc, currentState.stencilRef, currentState.stencilMask);
            GL11.glStencilOp(currentState.stencilFail, currentState.stencilZFail, currentState.stencilZPass);
        } else {
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
        
        // Apply cull state
        if (currentState.cullFaceEnabled) {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(currentState.cullFaceMode);
            GL11.glFrontFace(currentState.frontFace);
        } else {
            GL11.glDisable(GL11.GL_CULL_FACE);
        }
        
        // Apply scissor state
        if (currentState.scissorTestEnabled) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(currentState.scissorX, currentState.scissorY, 
                          currentState.scissorWidth, currentState.scissorHeight);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        
        // Apply viewport
        GL11.glViewport(currentState.viewportX, currentState.viewportY, 
                       currentState.viewportWidth, currentState.viewportHeight);
        
        // Apply clear color
        GL11.glClearColor(currentState.clearR, currentState.clearG, 
                         currentState.clearB, currentState.clearA);
        
        stateChanges++;
    }
    
    // State modification methods
    
    public void setBlendState(boolean enabled, int srcFactor, int dstFactor) {
        currentState.blendEnabled = enabled;
        currentState.blendSrcFactor = srcFactor;
        currentState.blendDstFactor = dstFactor;
    }
    
    public void setDepthState(boolean testEnabled, boolean writeEnabled, int func) {
        currentState.depthTestEnabled = testEnabled;
        currentState.depthWriteEnabled = writeEnabled;
        currentState.depthFunc = func;
    }
    
    public void setViewport(int x, int y, int width, int height) {
        currentState.viewportX = x;
        currentState.viewportY = y;
        currentState.viewportWidth = width;
        currentState.viewportHeight = height;
    }
    
    public void setClearColor(float r, float g, float b, float a) {
        currentState.clearR = r;
        currentState.clearG = g;
        currentState.clearB = b;
        currentState.clearA = a;
    }
    
    public void setScissorTest(boolean enabled, int x, int y, int width, int height) {
        currentState.scissorTestEnabled = enabled;
        currentState.scissorX = x;
        currentState.scissorY = y;
        currentState.scissorWidth = width;
        currentState.scissorHeight = height;
    }
    
    // Resource binding methods with state tracking
    
    public void bindShaderProgram(int program) {
        if (boundShaderProgram != program) {
            GL20.glUseProgram(program);
            boundShaderProgram = program;
            shaderBinds++;
        }
    }
    
    public void bindVertexArray(int vao) {
        if (boundVertexArray != vao) {
            GL30.glBindVertexArray(vao);
            boundVertexArray = vao;
        }
    }
    
    public void bindTexture(int target, int texture) {
        bindTexture(target, texture, activeTextureUnit);
    }
    
    public void bindTexture(int target, int texture, int unit) {
        if (unit >= boundTextures.length) {
            throw new IllegalArgumentException("Texture unit out of range: " + unit);
        }
        
        if (activeTextureUnit != unit) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            activeTextureUnit = unit;
        }
        
        if (boundTextures[unit] != texture) {
            GL11.glBindTexture(target, texture);
            boundTextures[unit] = texture;
            textureBinds++;
        }
    }
    
    public void bindFramebuffer(int framebuffer) {
        if (boundFramebuffer != framebuffer) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            boundFramebuffer = framebuffer;
        }
    }
    
    // Drawing methods with statistics
    
    public void drawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
        drawCalls++;
    }
    
    public void drawElements(int mode, int count, int type, long indices) {
        GL11.glDrawElements(mode, count, type, indices);
        drawCalls++;
    }
    
    public void clear(int mask) {
        GL11.glClear(mask);
    }
    
    // Frame management
    
    public void beginFrame() {
        frameCount++;
        drawCalls = 0;
        stateChanges = 0;
        textureBinds = 0;
        shaderBinds = 0;
    }
    
    public void endFrame() {
        // Frame cleanup if needed
    }
    
    // Getters
    
    public RenderState getCurrentState() {
        return currentState;
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public long getFrameCount() {
        return frameCount;
    }
    
    public long getDrawCalls() {
        return drawCalls;
    }
    
    public long getStateChanges() {
        return stateChanges;
    }
    
    public long getTextureBinds() {
        return textureBinds;
    }
    
    public long getShaderBinds() {
        return shaderBinds;
    }
    
    public int getBoundShaderProgram() {
        return boundShaderProgram;
    }
    
    public int getBoundVertexArray() {
        return boundVertexArray;
    }
    
    public int getBoundFramebuffer() {
        return boundFramebuffer;
    }
    
    public int getActiveTextureUnit() {
        return activeTextureUnit;
    }
    
    /**
     * Get render statistics as a formatted string
     */
    public String getStatistics() {
        return String.format("Frame: %d, DrawCalls: %d, StateChanges: %d, TextureBinds: %d, ShaderBinds: %d", 
                frameCount, drawCalls, stateChanges, textureBinds, shaderBinds);
    }
    
    @Override
    public String toString() {
        return "RenderContext[" + getStatistics() + "]";
    }
}