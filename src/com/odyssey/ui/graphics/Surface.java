package com.odyssey.ui.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import java.nio.ByteBuffer;

/**
 * Surface class for off-screen rendering and surface management
 * Provides framebuffer abstraction similar to modern graphics APIs
 */
public class Surface {
    private static final String TAG = "Surface";
    
    // Surface types
    public enum SurfaceType {
        COLOR_ONLY,
        COLOR_DEPTH,
        COLOR_DEPTH_STENCIL,
        DEPTH_ONLY,
        STENCIL_ONLY
    }
    
    // Surface formats
    public enum SurfaceFormat {
        RGBA8(GL11.GL_RGBA, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE),
        RGB8(GL11.GL_RGB, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE),
        RGBA16F(GL30.GL_RGBA16F, GL11.GL_RGBA, GL11.GL_FLOAT),
        RGBA32F(GL30.GL_RGBA32F, GL11.GL_RGBA, GL11.GL_FLOAT),
        DEPTH24(GL30.GL_DEPTH_COMPONENT24, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT),
        DEPTH32F(GL30.GL_DEPTH_COMPONENT32F, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT),
        DEPTH24_STENCIL8(GL30.GL_DEPTH24_STENCIL8, GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8);
        
        public final int internalFormat;
        public final int format;
        public final int type;
        
        SurfaceFormat(int internalFormat, int format, int type) {
            this.internalFormat = internalFormat;
            this.format = format;
            this.type = type;
        }
    }
    
    private int framebuffer;
    private int colorTexture;
    private int depthTexture;
    private int stencilTexture;
    private int depthStencilTexture;
    
    private final int width;
    private final int height;
    private final SurfaceType type;
    private final SurfaceFormat colorFormat;
    private final SurfaceFormat depthFormat;
    
    private boolean isCreated;
    private boolean isBound;
    private String name;
    
    // Saved viewport state
    private int[] savedViewport = new int[4];
    
    /**
     * Constructor for color-only surface
     */
    public Surface(int width, int height) {
        this(width, height, SurfaceType.COLOR_ONLY, SurfaceFormat.RGBA8, null);
    }
    
    /**
     * Constructor for color surface with depth
     */
    public Surface(int width, int height, SurfaceFormat colorFormat) {
        this(width, height, SurfaceType.COLOR_DEPTH, colorFormat, SurfaceFormat.DEPTH24);
    }
    
    /**
     * Full constructor
     */
    public Surface(int width, int height, SurfaceType type, SurfaceFormat colorFormat, SurfaceFormat depthFormat) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Surface dimensions must be positive");
        }
        
        this.width = width;
        this.height = height;
        this.type = type;
        this.colorFormat = colorFormat;
        this.depthFormat = depthFormat;
        this.name = "Surface_" + System.currentTimeMillis();
        this.isCreated = false;
        this.isBound = false;
    }
    
    /**
     * Create the surface resources
     */
    public void create() {
        if (isCreated) {
            throw new IllegalStateException("Surface already created");
        }
        
        // Generate framebuffer
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        
        try {
            // Create color attachment if needed
            if (type == SurfaceType.COLOR_ONLY || type == SurfaceType.COLOR_DEPTH || type == SurfaceType.COLOR_DEPTH_STENCIL) {
                createColorAttachment();
            }
            
            // Create depth attachment if needed
            if (type == SurfaceType.COLOR_DEPTH || type == SurfaceType.DEPTH_ONLY) {
                createDepthAttachment();
            }
            
            // Create depth-stencil attachment if needed
            if (type == SurfaceType.COLOR_DEPTH_STENCIL) {
                createDepthStencilAttachment();
            }
            
            // Create stencil attachment if needed
            if (type == SurfaceType.STENCIL_ONLY) {
                createStencilAttachment();
            }
            
            // Check framebuffer completeness
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Framebuffer not complete: " + getFramebufferStatusString(status));
            }
            
            isCreated = true;
            System.out.println("Surface created: " + this);
            
        } catch (Exception e) {
            // Clean up on failure
            destroy();
            throw e;
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }
    
    /**
     * Create color attachment
     */
    private void createColorAttachment() {
        colorTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, colorTexture);
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, colorFormat.internalFormat, 
                width, height, 0, colorFormat.format, colorFormat.type, (ByteBuffer) null);
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, 
                GL11.GL_TEXTURE_2D, colorTexture, 0);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Create depth attachment
     */
    private void createDepthAttachment() {
        depthTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTexture);
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, depthFormat.internalFormat, 
                width, height, 0, depthFormat.format, depthFormat.type, (ByteBuffer) null);
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, 
                GL11.GL_TEXTURE_2D, depthTexture, 0);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Create depth-stencil attachment
     */
    private void createDepthStencilAttachment() {
        depthStencilTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthStencilTexture);
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, SurfaceFormat.DEPTH24_STENCIL8.internalFormat, 
                width, height, 0, SurfaceFormat.DEPTH24_STENCIL8.format, 
                SurfaceFormat.DEPTH24_STENCIL8.type, (ByteBuffer) null);
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, 
                GL11.GL_TEXTURE_2D, depthStencilTexture, 0);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Create stencil attachment
     */
    private void createStencilAttachment() {
        stencilTexture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, stencilTexture);
        
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_STENCIL_INDEX8, 
                width, height, 0, GL11.GL_STENCIL_INDEX, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, 
                GL11.GL_TEXTURE_2D, stencilTexture, 0);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Bind the surface for rendering
     */
    public void bind() {
        if (!isCreated) {
            throw new IllegalStateException("Surface not created");
        }
        if (isBound) {
            throw new IllegalStateException("Surface already bound");
        }
        
        // Save current viewport
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, savedViewport);
        
        // Bind framebuffer and set viewport
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(0, 0, width, height);
        
        isBound = true;
    }
    
    /**
     * Unbind the surface
     */
    public void unbind() {
        if (!isBound) {
            return;
        }
        
        // Restore default framebuffer and viewport
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
        
        isBound = false;
    }
    
    /**
     * Clear the surface
     */
    public void clear(float r, float g, float b, float a) {
        if (!isBound) {
            throw new IllegalStateException("Surface not bound");
        }
        
        GL11.glClearColor(r, g, b, a);
        int mask = 0;
        
        if (colorTexture != 0) {
            mask |= GL11.GL_COLOR_BUFFER_BIT;
        }
        if (depthTexture != 0 || depthStencilTexture != 0) {
            mask |= GL11.GL_DEPTH_BUFFER_BIT;
        }
        if (stencilTexture != 0 || depthStencilTexture != 0) {
            mask |= GL11.GL_STENCIL_BUFFER_BIT;
        }
        
        if (mask != 0) {
            GL11.glClear(mask);
        }
    }
    
    /**
     * Blit this surface to another surface
     */
    public void blitTo(Surface target) {
        blitTo(target, 0, 0, width, height, 0, 0, target.width, target.height);
    }
    
    /**
     * Blit this surface to another surface with specified regions
     */
    public void blitTo(Surface target, int srcX, int srcY, int srcWidth, int srcHeight, 
                      int dstX, int dstY, int dstWidth, int dstHeight) {
        if (!isCreated || !target.isCreated) {
            throw new IllegalStateException("Both surfaces must be created");
        }
        
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.framebuffer);
        
        int mask = GL11.GL_COLOR_BUFFER_BIT;
        if (depthTexture != 0 && target.depthTexture != 0) {
            mask |= GL11.GL_DEPTH_BUFFER_BIT;
        }
        if (stencilTexture != 0 && target.stencilTexture != 0) {
            mask |= GL11.GL_STENCIL_BUFFER_BIT;
        }
        
        GL30.glBlitFramebuffer(srcX, srcY, srcX + srcWidth, srcY + srcHeight,
                              dstX, dstY, dstX + dstWidth, dstY + dstHeight,
                              mask, GL11.GL_LINEAR);
        
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Destroy the surface and free resources
     */
    public void destroy() {
        if (isBound) {
            unbind();
        }
        
        if (colorTexture != 0) {
            GL11.glDeleteTextures(colorTexture);
            colorTexture = 0;
        }
        
        if (depthTexture != 0) {
            GL11.glDeleteTextures(depthTexture);
            depthTexture = 0;
        }
        
        if (stencilTexture != 0) {
            GL11.glDeleteTextures(stencilTexture);
            stencilTexture = 0;
        }
        
        if (depthStencilTexture != 0) {
            GL11.glDeleteTextures(depthStencilTexture);
            depthStencilTexture = 0;
        }
        
        if (framebuffer != 0) {
            GL30.glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
        }
        
        isCreated = false;
        System.out.println("Surface destroyed: " + name);
    }
    
    /**
     * Get framebuffer status string
     */
    private String getFramebufferStatusString(int status) {
        switch (status) {
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
            case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
            case GL30.GL_FRAMEBUFFER_UNSUPPORTED:
                return "GL_FRAMEBUFFER_UNSUPPORTED";
            case GL32.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                return "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
            default:
                return "Unknown status: " + status;
        }
    }
    
    // Getters
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public SurfaceType getType() {
        return type;
    }
    
    public SurfaceFormat getColorFormat() {
        return colorFormat;
    }
    
    public SurfaceFormat getDepthFormat() {
        return depthFormat;
    }
    
    public int getFramebuffer() {
        return framebuffer;
    }
    
    public int getColorTexture() {
        return colorTexture;
    }
    
    public int getDepthTexture() {
        return depthTexture;
    }
    
    public int getStencilTexture() {
        return stencilTexture;
    }
    
    public int getDepthStencilTexture() {
        return depthStencilTexture;
    }
    
    public boolean isCreated() {
        return isCreated;
    }
    
    public boolean isBound() {
        return isBound;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return String.format("Surface[name=%s, size=%dx%d, type=%s, created=%b, bound=%b]", 
                name, width, height, type, isCreated, isBound);
    }
}