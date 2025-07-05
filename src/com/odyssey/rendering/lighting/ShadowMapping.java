package com.odyssey.rendering.lighting;

import com.odyssey.rendering.scene.Light;
import com.odyssey.rendering.Camera;
import com.odyssey.rendering.scene.RenderObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import static org.lwjgl.opengl.GL45.*;

public class ShadowMapping {
    // Cascade shadow mapping configuration
    private static final int CASCADE_COUNT = 4;
    private static final int SHADOW_MAP_SIZE = 2048;
    private static final float[] CASCADE_SPLITS = {0.1f, 15.0f, 50.0f, 150.0f, 500.0f};
    
    // Shadow map resources
    private int shadowMapFBO;
    private int shadowMapArray; // 2D texture array for cascades
    private int shadowMapShader;
    
    // Cascade data
    private Matrix4f[] lightSpaceMatrices;
    private float[] cascadeDistances;
    
    // Uniform buffer for shadow data
    private int shadowUBO;
    
    public ShadowMapping(int width, int height) {
        lightSpaceMatrices = new Matrix4f[CASCADE_COUNT];
        cascadeDistances = new float[CASCADE_COUNT];
        
        for (int i = 0; i < CASCADE_COUNT; i++) {
            lightSpaceMatrices[i] = new Matrix4f();
            cascadeDistances[i] = CASCADE_SPLITS[i + 1];
        }
        
        setupShadowMap();
        setupUniformBuffer();
        loadShaders();
        
        System.out.println("Cascade Shadow Mapping initialized with " + CASCADE_COUNT + " cascades");
    }
    
    private void setupShadowMap() {
        // Create framebuffer
        shadowMapFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, shadowMapFBO);
        
        // Create 2D texture array for cascade shadow maps
        shadowMapArray = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowMapArray);
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_DEPTH_COMPONENT32F, 
                    SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, CASCADE_COUNT, 
                    0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        
        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        
        // Set border color to white (outside shadow map = no shadow)
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        glTexParameterfv(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BORDER_COLOR, borderColor);
        
        // Enable shadow comparison
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        
        // Attach to framebuffer
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowMapArray, 0);
        
        // No color buffer needed
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        
        // Check framebuffer completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Shadow map framebuffer not complete!");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void setupUniformBuffer() {
        shadowUBO = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, shadowUBO);
        
        // Buffer size: 4 matrices (64 bytes each) + 4 floats (cascade distances)
        int bufferSize = CASCADE_COUNT * 64 + CASCADE_COUNT * 4;
        glBufferData(GL_UNIFORM_BUFFER, bufferSize, GL_DYNAMIC_DRAW);
        
        // Bind to binding point 2 (assuming 0=camera, 1=lights)
        glBindBufferBase(GL_UNIFORM_BUFFER, 2, shadowUBO);
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    
    private void loadShaders() {
        // Placeholder shader ID
        shadowMapShader = 20;
    }
    
    public void renderShadowMaps(List<Light> lights, List<RenderObject> objects) {
        // Find the primary directional light
        Light directionalLight = null;
        for (Light light : lights) {
            if (light.getLightType() == Light.LightType.DIRECTIONAL) {
                directionalLight = light;
                break;
            }
        }
        
        if (directionalLight == null) {
            return; // No directional light to cast shadows
        }
        
        // Calculate cascade matrices
        calculateCascadeMatrices(directionalLight, null);
        
        // Update uniform buffer
        updateShadowUBO();
        
        // Render shadow maps for each cascade
        glBindFramebuffer(GL_FRAMEBUFFER, shadowMapFBO);
        glViewport(0, 0, SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
        
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        
        // Enable polygon offset to reduce shadow acne
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(2.0f, 4.0f);
        
        glUseProgram(shadowMapShader);
        
        for (int cascade = 0; cascade < CASCADE_COUNT; cascade++) {
            // Bind specific layer of texture array
            glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, 
                                    shadowMapArray, 0, cascade);
            
            glClear(GL_DEPTH_BUFFER_BIT);
            
            // Set light space matrix for this cascade
            int lightSpaceLocation = glGetUniformLocation(shadowMapShader, "lightSpaceMatrix");
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer matrixBuffer = stack.mallocFloat(16);
                glUniformMatrix4fv(lightSpaceLocation, false, lightSpaceMatrices[cascade].get(matrixBuffer));
            }
            
            // Render all shadow-casting objects
            renderShadowCasters(objects);
        }
        
        glDisable(GL_POLYGON_OFFSET_FILL);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void calculateCascadeMatrices(Light directionalLight, Camera camera) {
        // Placeholder implementation - would need actual camera for proper cascade calculation
        Vector3f lightDir = new Vector3f(directionalLight.getDirection()).normalize();
        
        for (int i = 0; i < CASCADE_COUNT; i++) {
            float nearPlane = CASCADE_SPLITS[i];
            float farPlane = CASCADE_SPLITS[i + 1];
            
            // Simple orthographic projection for now
            float size = farPlane * 0.5f;
            Matrix4f lightView = new Matrix4f().lookAt(
                new Vector3f(lightDir).mul(-100.0f), 
                new Vector3f(0, 0, 0), 
                new Vector3f(0, 1, 0)
            );
            
            Matrix4f lightProj = new Matrix4f().ortho(-size, size, -size, size, 0.1f, 200.0f);
            
            lightSpaceMatrices[i] = new Matrix4f(lightProj).mul(lightView);
        }
    }
    
    private void renderShadowCasters(List<RenderObject> renderObjects) {
        // Placeholder for rendering shadow-casting objects
        // In a real implementation, this would render all opaque geometry
        // using the shadow map shader (vertex transformation only)
    }
    
    private void updateShadowUBO() {
        glBindBuffer(GL_UNIFORM_BUFFER, shadowUBO);
        
        // Upload light space matrices
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < CASCADE_COUNT; i++) {
                FloatBuffer matrixBuffer = stack.mallocFloat(16);
                lightSpaceMatrices[i].get(matrixBuffer);
                glBufferSubData(GL_UNIFORM_BUFFER, i * 64, matrixBuffer);
            }
        }
        
        // Upload cascade distances
        FloatBuffer cascadeBuffer = org.lwjgl.BufferUtils.createFloatBuffer(CASCADE_COUNT);
        cascadeBuffer.put(cascadeDistances);
        cascadeBuffer.flip();
        glBufferSubData(GL_UNIFORM_BUFFER, CASCADE_COUNT * 64, cascadeBuffer);
        
        glBindBuffer(GL_UNIFORM_BUFFER, 0);
    }
    
    // Getters for use by other rendering systems
    public int getShadowMapTexture() {
        return shadowMapArray;
    }
    
    public Matrix4f[] getLightSpaceMatrices() {
        return lightSpaceMatrices;
    }
    
    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrices[0]; // Return first cascade for simple shadow mapping
    }
    
    public float[] getCascadeDistances() {
        return cascadeDistances;
    }
    
    public int getCascadeCount() {
        return CASCADE_COUNT;
    }
    
    public void bindShadowMap(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowMapArray);
    }
    
    public void resize(int width, int height) {
        // Shadow maps don't need to resize with window size
        // They maintain their fixed resolution for consistent quality
        // This method is here for interface compatibility
    }

    public void cleanup() {
        glDeleteFramebuffers(shadowMapFBO);
        glDeleteTextures(shadowMapArray);
        glDeleteBuffers(shadowUBO);
    }
}