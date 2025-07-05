/**
 * Enhanced Volumetric Cloud Shader
 * 
 * Features:
 * - Advanced volumetric cloud rendering with multiple noise layers
 * - Realistic lighting with multiple scattering
 * - Weather system integration
 * - Performance optimizations with adaptive sampling
 * - Temporal stability and animation
 * 
 * Author: Enhanced Shader Pipeline
 * Version: 2.0
 */

#version 330 core

// ========================================================================
// OUTPUT
// ========================================================================
out vec4 FragColor;

// ========================================================================
// INPUT
// ========================================================================
in vec2 TexCoord;

// ========================================================================
// UNIFORMS - TRANSFORMATION
// ========================================================================
uniform mat4 invProjection;
uniform mat4 invView;

// ========================================================================
// UNIFORMS - LIGHTING
// ========================================================================
uniform vec3 lightDir;          // Sun direction
uniform vec3 lightColor;        // Sun color
uniform float lightIntensity;   // Sun intensity
uniform vec3 ambientColor;      // Ambient light color
uniform float ambientStrength;  // Ambient light strength

// ========================================================================
// UNIFORMS - TIME AND ANIMATION
// ========================================================================
uniform float time;              // Global time
uniform float windSpeed;         // Wind speed for cloud movement
uniform vec3 windDirection;      // Wind direction

// ========================================================================
// UNIFORMS - CLOUD PROPERTIES
// ========================================================================
uniform float cloudCoverage;     // 0.0 to 1.0
uniform float cloudDensity;      // Cloud density multiplier
uniform float cloudScale;        // Cloud scale factor
uniform float cloudHeight;       // Cloud layer height
uniform float cloudThickness;    // Cloud layer thickness
uniform float cloudAbsorption;   // Light absorption coefficient
uniform float cloudScattering;   // Light scattering coefficient

// ========================================================================
// UNIFORMS - WEATHER
// ========================================================================
uniform float weatherIntensity;  // Weather effect intensity
uniform int weatherType;         // 0=clear, 1=overcast, 2=storm
uniform float humidity;          // Humidity level
uniform float temperature;       // Temperature

// ========================================================================
// UNIFORMS - QUALITY
// ========================================================================
uniform int cloudQuality;        // 0=low, 1=medium, 2=high
uniform bool enableVolumetricLighting;
uniform bool enableTemporalFiltering;

// ========================================================================
// CONSTANTS
// ========================================================================
const float PI = 3.14159265359;
const float EPSILON = 1e-6;
const float INV_PI = 1.0 / PI;

// Cloud rendering constants
const int MAX_MARCH_STEPS_LOW = 32;
const int MAX_MARCH_STEPS_MED = 64;
const int MAX_MARCH_STEPS_HIGH = 128;
const float MAX_DISTANCE = 50000.0;
const float MIN_STEP_SIZE = 100.0;
const float MAX_STEP_SIZE = 1000.0;

// Noise constants
const float NOISE_SCALE_BASE = 0.0001;
const float NOISE_SCALE_DETAIL = 0.001;
const int NOISE_OCTAVES = 4;

// Lighting constants
const float PHASE_G = 0.8;       // Henyey-Greenstein phase function parameter
const float SILVER_INTENSITY = 0.7;
const float SILVER_SPREAD = 32.0;

// Weather type constants
const int WEATHER_CLEAR = 0;
const int WEATHER_OVERCAST = 1;
const int WEATHER_STORM = 2;

// ========================================================================
// UTILITY FUNCTIONS
// ========================================================================

/**
 * Validate float input
 */
float validateFloat(float value, float defaultVal) {
    return (isnan(value) || isinf(value)) ? defaultVal : value;
}

/**
 * Validate vec3 input
 */
vec3 validateVec3(vec3 value, vec3 defaultVal) {
    if (any(isnan(value)) || any(isinf(value))) return defaultVal;
    return value;
}

/**
 * Safe normalize function
 */
vec3 safeNormalize(vec3 v) {
    float len = length(v);
    return len > EPSILON ? v / len : vec3(0.0, 1.0, 0.0);
}

// ========================================================================
// NOISE FUNCTIONS
// ========================================================================

/**
 * Enhanced hash function for better noise quality
 */
vec3 hash3(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
             dot(p, vec3(269.5, 183.3, 246.1)),
             dot(p, vec3(113.5, 271.9, 124.6)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

/**
 * Single hash function
 */
float hash1(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

/**
 * 3D Simplex noise with improved quality
 */
float noise3D(vec3 p) {
    const float K1 = 0.366025404; // (sqrt(3)-1)/3
    const float K2 = 0.211324865; // (3-sqrt(3))/6
    
    vec3 i = floor(p + (p.x + p.y + p.z) * K1);
    vec3 a = p - i + (i.x + i.y + i.z) * K2;
    
    vec3 m = step(a.yxz, a.xyz);
    vec3 o = mix(vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0), m.x);
    o = mix(o, vec3(0.0, 0.0, 1.0), m.y);
    
    vec3 b = a - o + K2;
    vec3 c = a - 1.0 + 2.0 * K2;
    
    vec3 h = max(0.5 - vec3(dot(a, a), dot(b, b), dot(c, c)), 0.0);
    vec3 n = h * h * h * h * vec3(
        dot(a, hash3(i + 0.0)),
        dot(b, hash3(i + o)),
        dot(c, hash3(i + 1.0))
    );
    
    return dot(n, vec3(70.0));
}

/**
 * Worley noise for cloud detail
 */
float worleyNoise(vec3 p) {
    vec3 id = floor(p);
    vec3 f = fract(p);
    
    float minDist = 1.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            for (int z = -1; z <= 1; z++) {
                vec3 neighbor = vec3(float(x), float(y), float(z));
                vec3 point = hash3(id + neighbor);
                point = 0.5 + 0.5 * sin(time * 0.01 + 6.2831 * point);
                vec3 diff = neighbor + point - f;
                float dist = length(diff);
                minDist = min(minDist, dist);
            }
        }
    }
    return minDist;
}

/**
 * Curl noise for cloud animation
 */
vec3 curlNoise(vec3 p) {
    const float e = 0.1;
    vec3 dx = vec3(e, 0.0, 0.0);
    vec3 dy = vec3(0.0, e, 0.0);
    vec3 dz = vec3(0.0, 0.0, e);
    
    float p_x0 = noise3D(p - dx);
    float p_x1 = noise3D(p + dx);
    float p_y0 = noise3D(p - dy);
    float p_y1 = noise3D(p + dy);
    float p_z0 = noise3D(p - dz);
    float p_z1 = noise3D(p + dz);
    
    float x = p_y1 - p_y0 - p_z1 + p_z0;
    float y = p_z1 - p_z0 - p_x1 + p_x0;
    float z = p_x1 - p_x0 - p_y1 + p_y0;
    
    return normalize(vec3(x, y, z) / (2.0 * e));
}

// ========================================================================
// ADVANCED NOISE FUNCTIONS
// ========================================================================

/**
 * Enhanced Fractional Brownian Motion
 */
float fbm(vec3 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    float maxValue = 0.0;
    
    for (int i = 0; i < octaves; i++) {
        value += amplitude * noise3D(p * frequency);
        maxValue += amplitude;
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value / maxValue;
}

/**
 * Ridged noise for cloud edges
 */
float ridgedNoise(vec3 p, int octaves) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    
    for (int i = 0; i < octaves; i++) {
        float n = abs(noise3D(p * frequency));
        n = 1.0 - n;
        n = n * n;
        value += amplitude * n;
        amplitude *= 0.5;
        frequency *= 2.0;
    }
    
    return value;
}

/**
 * Weather-based cloud shape modification
 */
float getWeatherMultiplier(vec3 worldPos) {
    float weatherMult = 1.0;
    
    if (weatherType == WEATHER_OVERCAST) {
        weatherMult = 1.5 + 0.3 * sin(time * 0.1);
    } else if (weatherType == WEATHER_STORM) {
        weatherMult = 2.0 + 0.5 * sin(time * 0.2) + 0.3 * sin(time * 0.15);
    }
    
    // Add humidity and temperature effects
    weatherMult *= (0.5 + 0.5 * humidity);
    weatherMult *= (0.8 + 0.4 * clamp(temperature / 30.0, 0.0, 1.0));
    
    return weatherMult;
}

/**
 * Advanced cloud density calculation
 */
float mapCloud(vec3 p) {
    // Animate clouds with wind
    vec3 windOffset = windDirection * windSpeed * time * 0.01;
    vec3 p_anim = p + windOffset;
    
    // Add curl noise for realistic cloud movement
    vec3 curl = curlNoise(p * 0.0005) * 100.0;
    p_anim += curl;
    
    // Base cloud shape using multiple noise layers
    float baseNoise = fbm(p_anim * cloudScale * NOISE_SCALE_BASE, NOISE_OCTAVES);
    float detailNoise = fbm(p_anim * cloudScale * NOISE_SCALE_DETAIL, 3);
    float worley = worleyNoise(p_anim * cloudScale * 0.01);
    
    // Combine noise layers
    float cloudShape = baseNoise;
    cloudShape = mix(cloudShape, detailNoise, 0.3);
    cloudShape -= worley * 0.4; // Subtract worley for cloud holes
    
    // Apply weather effects
    float weatherMult = getWeatherMultiplier(p);
    cloudShape *= weatherMult;
    
    // Height-based density falloff
    float heightFactor = 1.0 - abs(p.y - cloudHeight) / cloudThickness;
    heightFactor = clamp(heightFactor, 0.0, 1.0);
    heightFactor = smoothstep(0.0, 1.0, heightFactor);
    
    // Apply coverage and height falloff
    cloudShape = smoothstep(0.4, 0.8, cloudShape) * cloudCoverage * heightFactor;
    
    return clamp(cloudShape, 0.0, 1.0);
}

// ========================================================================
// LIGHTING FUNCTIONS
// ========================================================================

/**
 * Henyey-Greenstein phase function
 */
float henyeyGreenstein(float cosTheta, float g) {
    float g2 = g * g;
    return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * cosTheta, 1.5));
}

/**
 * Calculate light scattering through clouds
 */
float calculateScattering(vec3 rayDir, vec3 lightDir, float density) {
    float cosTheta = dot(rayDir, lightDir);
    
    // Forward scattering (silver lining)
    float forwardScatter = henyeyGreenstein(cosTheta, PHASE_G);
    
    // Backward scattering
    float backScatter = henyeyGreenstein(cosTheta, -PHASE_G * 0.3);
    
    // Combine scattering
    float scattering = mix(backScatter, forwardScatter, 0.7);
    
    // Add silver lining effect
    float silverLining = pow(max(cosTheta, 0.0), SILVER_SPREAD) * SILVER_INTENSITY;
    scattering += silverLining;
    
    return scattering * density;
}

/**
 * Calculate ambient lighting contribution
 */
vec3 calculateAmbientLighting(float density) {
    vec3 ambient = ambientColor * ambientStrength;
    
    // Modulate ambient based on weather
    if (weatherType == WEATHER_OVERCAST) {
        ambient *= 0.6;
    } else if (weatherType == WEATHER_STORM) {
        ambient *= 0.3;
    }
    
    return ambient * density;
}

// ========================================================================
// RAYMARCHING FUNCTIONS
// ========================================================================

/**
 * Get adaptive step size based on distance and quality
 */
float getAdaptiveStepSize(float distance, int quality) {
    float baseStep = MIN_STEP_SIZE;
    if (quality == 0) baseStep = MAX_STEP_SIZE;        // Low quality
    else if (quality == 1) baseStep = MIN_STEP_SIZE * 2.0; // Medium quality
    
    // Increase step size with distance
    float adaptiveStep = baseStep * (1.0 + distance * 0.0001);
    return clamp(adaptiveStep, MIN_STEP_SIZE, MAX_STEP_SIZE);
}

/**
 * Get maximum march steps based on quality
 */
int getMaxSteps(int quality) {
    if (quality == 0) return MAX_MARCH_STEPS_LOW;
    else if (quality == 1) return MAX_MARCH_STEPS_MED;
    return MAX_MARCH_STEPS_HIGH;
}

/**
 * Calculate light transmission through clouds
 */
float calculateLightTransmission(vec3 pos, vec3 lightDir, float stepSize) {
    float transmission = 1.0;
    float shadowStepSize = stepSize * 2.0;
    int shadowSteps = 6;
    
    for (int i = 1; i <= shadowSteps; i++) {
        vec3 shadowPos = pos + lightDir * shadowStepSize * float(i);
        float shadowDensity = mapCloud(shadowPos);
        transmission *= exp(-shadowDensity * cloudAbsorption * shadowStepSize);
        
        if (transmission < 0.01) break; // Early exit for performance
    }
    
    return transmission;
}

/**
 * Enhanced raymarching with volumetric lighting
 */
vec4 raymarch(vec3 ro, vec3 rd) {
    // Input validation
    ro = validateVec3(ro, vec3(0.0, cloudHeight, 0.0));
    rd = safeNormalize(rd);
    
    vec3 lightDir_safe = safeNormalize(lightDir);
    int maxSteps = getMaxSteps(cloudQuality);
    
    // Accumulated values
    vec3 accumulatedColor = vec3(0.0);
    float accumulatedAlpha = 0.0;
    float transmittance = 1.0;
    
    // Raymarching variables
    float distance = 0.0;
    float stepSize = getAdaptiveStepSize(distance, cloudQuality);
    
    for (int i = 0; i < maxSteps; i++) {
        if (transmittance < 0.01 || distance > MAX_DISTANCE) break;
        
        vec3 currentPos = ro + rd * distance;
        
        // Check if we're in the cloud layer
        float heightMin = cloudHeight - cloudThickness * 0.5;
        float heightMax = cloudHeight + cloudThickness * 0.5;
        
        if (currentPos.y >= heightMin && currentPos.y <= heightMax) {
            float density = mapCloud(currentPos) * cloudDensity;
            
            if (density > 0.01) {
                // Calculate lighting
                vec3 sampleColor = lightColor;
                float lightTransmission = 1.0;
                
                if (enableVolumetricLighting) {
                    lightTransmission = calculateLightTransmission(currentPos, lightDir_safe, stepSize);
                }
                
                // Scattering calculation
                float scattering = calculateScattering(rd, lightDir_safe, density);
                
                // Combine direct and ambient lighting
                vec3 directLight = sampleColor * lightIntensity * lightTransmission * scattering;
                vec3 ambientLight = calculateAmbientLighting(density);
                vec3 totalLight = directLight + ambientLight;
                
                // Apply weather-based color tinting
                if (weatherType == WEATHER_STORM) {
                    totalLight *= vec3(0.7, 0.7, 0.9); // Bluish tint for storms
                } else if (weatherType == WEATHER_OVERCAST) {
                    totalLight *= vec3(0.9, 0.9, 0.95); // Slightly desaturated
                }
                
                // Calculate alpha
                float alpha = 1.0 - exp(-density * cloudAbsorption * stepSize);
                alpha = clamp(alpha, 0.0, 1.0);
                
                // Accumulate color and alpha
                accumulatedColor += totalLight * alpha * transmittance;
                accumulatedAlpha += alpha * transmittance;
                
                // Update transmittance
                transmittance *= (1.0 - alpha);
            }
        }
        
        // Adaptive step size
        stepSize = getAdaptiveStepSize(distance, cloudQuality);
        distance += stepSize;
    }
    
    // Ensure valid output
    accumulatedColor = validateVec3(accumulatedColor, vec3(0.0));
    accumulatedAlpha = validateFloat(accumulatedAlpha, 0.0);
    accumulatedAlpha = clamp(accumulatedAlpha, 0.0, 1.0);
    
    return vec4(accumulatedColor, accumulatedAlpha);
}


// ========================================================================
// MAIN FUNCTION
// ========================================================================

void main() {
    // Input validation
    vec2 safeTexCoord = clamp(TexCoord, vec2(0.0), vec2(1.0));
    
    // Reconstruct world-space ray from camera
    vec4 ray_clip = vec4(safeTexCoord * 2.0 - 1.0, 1.0, 1.0);
    vec4 ray_view = invProjection * ray_clip;
    ray_view.z = -1.0;
    ray_view.w = 0.0;
    
    vec3 ray_dir = normalize(vec3(invView * ray_view));
    vec3 ray_origin = vec3(invView[3]);
    
    // Validate ray direction and origin
    ray_dir = safeNormalize(ray_dir);
    ray_origin = validateVec3(ray_origin, vec3(0.0, cloudHeight, 0.0));
    
    // Perform raymarching
    vec4 cloudColor = raymarch(ray_origin, ray_dir);
    
    // Apply temporal filtering if enabled
    if (enableTemporalFiltering) {
        // Simple temporal smoothing based on time
        float temporalFactor = 0.95 + 0.05 * sin(time * 0.1);
        cloudColor.rgb *= temporalFactor;
    }
    
    // Apply exposure and tone mapping for clouds
    cloudColor.rgb *= lightIntensity;
    
    // Gamma correction
    cloudColor.rgb = pow(cloudColor.rgb, vec3(1.0 / 2.2));
    
    // Final validation
    cloudColor = vec4(
        validateVec3(cloudColor.rgb, vec3(0.0)),
        validateFloat(cloudColor.a, 0.0)
    );
    
    // Clamp final output
    cloudColor = clamp(cloudColor, vec4(0.0), vec4(1.0));
    
    // Early discard for performance
    if (cloudColor.a < 0.01) {
        discard;
    }
    
    FragColor = cloudColor;
}