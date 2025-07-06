package com.odyssey.rendering;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Utility class for comprehensive OpenGL error checking
 * Helps detect silent OpenGL failures that could cause black screen issues
 */
public class GLErrorChecker {
    
    private static boolean errorCheckingEnabled = true;
    
    /**
     * Check for OpenGL errors and log them with context
     */
    public static void checkGLError(String context) {
        if (!errorCheckingEnabled) return;
        
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            String errorString = getErrorString(error);
            String message = "OpenGL Error in " + context + ": " + errorString + " (0x" + Integer.toHexString(error) + ")";
            System.err.println(message);
            
            // Print stack trace to help identify the source
            Thread.currentThread().getStackTrace();
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (element.getClassName().startsWith("com.odyssey")) {
                    System.err.println("  at " + element);
                    break;
                }
            }
            
            // For critical errors, consider throwing an exception
            if (error == GL_OUT_OF_MEMORY) {
                throw new RuntimeException("OpenGL out of memory in " + context);
            }
        }
    }
    
    /**
     * Check for OpenGL errors silently (no logging)
     * Returns true if there was an error
     */
    public static boolean hasGLError() {
        return glGetError() != GL_NO_ERROR;
    }
    
    /**
     * Clear any pending OpenGL errors
     */
    public static void clearGLErrors() {
        while (glGetError() != GL_NO_ERROR) {
            // Clear all pending errors
        }
    }
    
    /**
     * Check framebuffer completeness
     */
    public static boolean checkFramebufferComplete(String context) {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String statusString = getFramebufferStatusString(status);
            System.err.println("Framebuffer not complete in " + context + ": " + statusString);
            return false;
        }
        return true;
    }
    
    /**
     * Validate shader program
     */
    public static boolean validateShaderProgram(int program, String context) {
        if (program == 0) {
            System.err.println("Invalid shader program (0) in " + context);
            return false;
        }
        
        if (!glIsProgram(program)) {
            System.err.println("Invalid shader program " + program + " in " + context);
            return false;
        }
        
        // Check if program is linked
        int linkStatus = glGetProgrami(program, GL_LINK_STATUS);
        if (linkStatus == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            System.err.println("Shader program not linked in " + context + ": " + log);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate uniform location
     */
    public static boolean validateUniformLocation(int location, String uniformName, String context) {
        if (location == -1) {
            System.err.println("Uniform '" + uniformName + "' not found in " + context);
            return false;
        }
        return true;
    }
    
    /**
     * Validate texture binding
     */
    public static boolean validateTexture(int texture, String context) {
        if (texture == 0) {
            System.err.println("Invalid texture (0) in " + context);
            return false;
        }
        
        if (!glIsTexture(texture)) {
            System.err.println("Invalid texture " + texture + " in " + context);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate VAO binding
     */
    public static boolean validateVAO(int vao, String context) {
        if (vao == 0) {
            System.err.println("Invalid VAO (0) in " + context);
            return false;
        }
        
        if (!glIsVertexArray(vao)) {
            System.err.println("Invalid VAO " + vao + " in " + context);
            return false;
        }
        
        return true;
    }
    
    /**
     * Enable or disable error checking (for performance)
     */
    public static void setErrorCheckingEnabled(boolean enabled) {
        errorCheckingEnabled = enabled;
    }
    
    private static String getErrorString(int error) {
        switch (error) {
            case GL_NO_ERROR:
                return "GL_NO_ERROR";
            case GL_INVALID_ENUM:
                return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE:
                return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION:
                return "GL_INVALID_OPERATION";
            case GL_OUT_OF_MEMORY:
                return "GL_OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION:
                return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default:
                return "UNKNOWN_ERROR";
        }
    }
    
    private static String getFramebufferStatusString(int status) {
        switch (status) {
            case GL_FRAMEBUFFER_COMPLETE:
                return "GL_FRAMEBUFFER_COMPLETE";
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
            case GL_FRAMEBUFFER_UNSUPPORTED:
                return "GL_FRAMEBUFFER_UNSUPPORTED";
            case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                return "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
            default:
                return "UNKNOWN_STATUS: 0x" + Integer.toHexString(status);
        }
    }
} 