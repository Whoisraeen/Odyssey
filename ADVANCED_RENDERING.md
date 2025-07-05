# Advanced Rendering Pipeline

This document describes the advanced rendering pipeline implementation for the Odyssey voxel engine.

## Overview

The advanced rendering pipeline implements a modern deferred rendering approach with the following features:

- **Deferred Rendering**: G-Buffer based rendering for efficient lighting calculations
- **Physically Based Rendering (PBR)**: Cook-Torrance BRDF with metallic/roughness workflow
- **Screen Space Ambient Occlusion (SSAO)**: Enhanced depth perception and realism
- **Shadow Mapping**: Cascade shadow maps for directional lights
- **Volumetric Lighting**: Atmospheric scattering effects
- **Post-Processing**: HDR tone mapping, bloom, and FXAA anti-aliasing

## Architecture

### Core Components

1. **AdvancedRenderingPipeline**: Main rendering coordinator
2. **LightingSystem**: Deferred lighting calculations
3. **ShadowMapping**: Shadow map generation and sampling
4. **VolumetricLighting**: Atmospheric effects
5. **PostProcessing**: HDR and post-processing effects
6. **Scene**: Scene graph management
7. **Material**: PBR material system

### Rendering Passes

1. **Shadow Pass**: Render shadow maps from light perspectives
2. **G-Buffer Pass**: Render scene geometry to G-Buffer textures
3. **SSAO Pass**: Calculate ambient occlusion
4. **Lighting Pass**: Deferred lighting calculations
5. **Volumetric Pass**: Atmospheric scattering
6. **Transparent Pass**: Forward rendering for transparent objects
7. **Post-Processing Pass**: HDR tone mapping, bloom, FXAA

### G-Buffer Layout

- **gPosition** (RGB16F): World space position
- **gNormal** (RGB16F): World space normal
- **gAlbedoSpec** (RGBA8): Albedo (RGB) + Specular intensity (A)
- **gMaterial** (RGB8): Metallic, Roughness, Ambient Occlusion

## Shader Files

### Geometry Shaders
- `geometry.vert`: Vertex shader for G-Buffer pass
- `geometry.frag`: Fragment shader for G-Buffer pass

### Lighting Shaders
- `deferred_lighting.vert`: Fullscreen quad vertex shader
- `deferred_lighting.frag`: Deferred lighting fragment shader

### Post-Processing Shaders
- `ssao.frag`: Screen Space Ambient Occlusion
- `ssao_blur.frag`: SSAO blur pass
- `tonemap.frag`: HDR tone mapping
- `bloom_extract.frag`: Bloom bright pass
- `bloom_blur.frag`: Bloom blur pass
- `fxaa.frag`: Fast Approximate Anti-Aliasing

### Shadow Shaders
- `shadow_map.vert`: Shadow map vertex shader
- `shadow_map.frag`: Shadow map fragment shader

### Volumetric Shaders
- `volumetric.frag`: Volumetric lighting fragment shader

### Utility Shaders
- `fullscreen.vert`: Fullscreen quad vertex shader

## Usage

### Basic Setup

```java
// Initialize the rendering pipeline
AdvancedRenderingPipeline pipeline = new AdvancedRenderingPipeline(width, height);

// Create a scene
Scene scene = new Scene();
scene.setupDefaultLighting();

// Add objects to the scene
RenderObject object = new MyRenderObject();
scene.addObject(object);

// Render
pipeline.render(scene, camera);
```

### Material Creation

```java
// Create a metallic material
Material metal = Material.createMetal(
    new Vector3f(0.7f, 0.7f, 0.8f), // albedo
    0.1f // roughness
);

// Create a dielectric material
Material dielectric = Material.createDielectric(
    new Vector3f(0.8f, 0.2f, 0.2f), // albedo
    0.5f // roughness
);

// Create an emissive material
Material emissive = Material.createEmissive(
    new Vector3f(1.0f, 0.5f, 0.0f), // color
    5.0f // strength
);
```

### Lighting Setup

```java
// Add a directional light (sun)
Light sun = Light.createDirectional(
    new Vector3f(-0.3f, -1.0f, -0.3f), // direction
    new Vector3f(1.0f, 0.95f, 0.8f),   // color
    3.0f // intensity
);
scene.addLight(sun);

// Add a point light
Light pointLight = Light.createPoint(
    new Vector3f(10.0f, 5.0f, 10.0f), // position
    new Vector3f(1.0f, 0.8f, 0.6f),   // color
    10.0f, // intensity
    20.0f  // radius
);
scene.addLight(pointLight);
```

## Performance Considerations

- **G-Buffer Size**: Uses 16-bit floating point for position/normal, 8-bit for albedo/material
- **Shadow Map Resolution**: Configurable cascade shadow map resolution
- **SSAO Samples**: Adjustable sample count for quality vs performance
- **Bloom Resolution**: Lower resolution bloom passes for better performance
- **Volumetric Resolution**: Rendered at quarter resolution by default

## Configuration

The pipeline can be configured through various parameters:

```java
// Shadow mapping configuration
shadowMapping.setCascadeCount(4);
shadowMapping.setShadowMapSize(2048);
shadowMapping.setCascadeSplits(new float[]{0.1f, 0.3f, 0.7f, 1.0f});

// SSAO configuration
lightingSystem.setSSAORadius(0.5f);
lightingSystem.setSSAOBias(0.025f);
lightingSystem.setSSAOSamples(64);

// Post-processing configuration
postProcessing.setExposure(1.0f);
postProcessing.setBloomThreshold(1.0f);
postProcessing.setBloomIntensity(0.04f);
```

## Testing

Run the test application to verify the pipeline:

```bash
java com.odyssey.test.RenderingPipelineTest
```

This will create a window with the voxel engine using the advanced rendering pipeline.

## Future Enhancements

- **Temporal Anti-Aliasing (TAA)**: Motion-based anti-aliasing
- **Screen Space Reflections (SSR)**: Real-time reflections
- **Global Illumination**: Light bouncing and indirect lighting
- **Clustered Deferred Rendering**: Support for many lights
- **Compute Shader Optimizations**: GPU-based culling and processing