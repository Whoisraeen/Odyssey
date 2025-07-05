#version 330 core

out vec4 FragColor;
in vec2 TexCoord;

// --- Uniforms ---
uniform mat4 invProjection;
uniform mat4 invView;
uniform vec3 lightDir; // Sun direction
uniform float time;

// Cloud properties controlled by weather
uniform float cloudCoverage; // 0.0 to 1.0
uniform float cloudDensity; 

// --- Constants ---
const int MAX_MARCH_STEPS = 64;
const float MAX_DISTANCE = 30000.0;
const float STEP_SIZE = 500.0;

// --- Noise Functions (3D Simplex Noise) ---
// Hashing function
vec3 hash( vec3 p ) {
    p = vec3( dot(p,vec3(127.1,311.7, 74.7)),
              dot(p,vec3(269.5,183.3,246.1)),
              dot(p,vec3(113.5,271.9,124.6)));
    return -1.0 + 2.0*fract(sin(p)*43758.5453123);
}

// 3D Simplex noise
float noise( in vec3 p ) {
    const float K1 = 0.366025404; // (sqrt(3)-1)/3
    const float K2 = 0.211324865; // (3-sqrt(3))/6
    
    vec3 i = floor(p + (p.x+p.y+p.z)*K1);
    vec3 a = p - i + (i.x+i.y+i.z)*K2;
    vec3 m = step(a.yxz, a.xyz); 
    vec3 o = mix(vec3(1,0,0), vec3(0,1,0), m.x);
    o = mix(o, vec3(0,0,1), m.y);
    vec3 b = a - o + K2;
    vec3 c = a - 1.0 + 2.0*K2;
    vec3 h = max(0.5 - vec3(dot(a,a), dot(b,b), dot(c,c)), 0.0);
    vec3 n = h*h*h*h*vec3( dot(a,hash(i+0.0)), dot(b,hash(i+o)), dot(c,hash(i+1.0)));
    return dot(n, vec3(70.0));
}

// --- Fractional Brownian Motion (for cloud shape) ---
float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    float frequency = 0.0;
    for (int i = 0; i < 4; i++) {
        value += amplitude * noise(p);
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

// --- Cloud Map Function ---
float mapCloud(vec3 p) {
    vec3 p_anim = p;
    p_anim.x += time * 0.01; // Animate clouds moving
    
    float base_fbm = fbm(p_anim * 0.0001);
    
    // Use a threshold to create cloud shapes
    float cloud_shape = smoothstep(0.45, 0.7, base_fbm);
    cloud_shape *= cloudCoverage;
    
    return cloud_shape;
}

// --- Raymarching Function ---
vec4 raymarch(vec3 ro, vec3 rd) {
    float totalDensity = 0.0;
    
    for (int i = 0; i < MAX_MARCH_STEPS; i++) {
        vec3 p = ro + rd * (float(i) * STEP_SIZE);
        
        // Only sample within a certain height band
        if (p.y > 1000.0 && p.y < 4000.0) {
            float densitySample = mapCloud(p);
            if (densitySample > 0.0) {
                // Simple lighting: more light gets through less dense parts
                float light_factor = pow(1.0 - densitySample, 2.0);
                totalDensity += densitySample * light_factor * cloudDensity;
            }
        }
    }
    
    return vec4(vec3(1.0), totalDensity); // White clouds for now
}


void main() {
    // Reconstruct world-space ray from camera
    vec4 ray_clip = vec4(TexCoord * 2.0 - 1.0, 1.0, 1.0);
    vec4 ray_view = invProjection * ray_clip;
    ray_view.z = -1.0; ray_view.w = 0.0;
    vec3 ray_dir = normalize(vec3(invView * ray_view));
    vec3 ray_origin = vec3(invView[3]);

    vec4 cloudColor = raymarch(ray_origin, ray_dir);
    
    // If no clouds, discard fragment
    if (cloudColor.a < 0.01) {
        discard;
    }
    
    FragColor = cloudColor;
} 