package com.odyssey.rendering;

import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Texture {
    private int textureId;
    
    public Texture(String filePath) {
        this.textureId = loadTexture(filePath);
    }
    
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    public void cleanup() {
        glDeleteTextures(textureId);
    }
    
    public int getId() {
        return textureId;
    }
    
    public static int loadTexture(String resourcePath) {
        int textureID;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load from classpath resources
            ByteBuffer imageBuffer;
            try (InputStream stream = Texture.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    throw new RuntimeException("Can't find texture file at " + resourcePath);
                }
                
                // Read all bytes from the stream
                byte[] bytes = stream.readAllBytes();
                imageBuffer = memAlloc(bytes.length);
                imageBuffer.put(bytes);
                imageBuffer.flip();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load texture: " + resourcePath, e);
            }

            ByteBuffer image = stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            memFree(imageBuffer); // Free the temporary buffer
            
            if (image == null) {
                throw new RuntimeException("Failed to load texture file: " + resourcePath
                                           + System.lineSeparator() + stbi_failure_reason());
            }

            textureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureID);
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);
            
            stbi_image_free(image);
        }
        return textureID;
    }
}