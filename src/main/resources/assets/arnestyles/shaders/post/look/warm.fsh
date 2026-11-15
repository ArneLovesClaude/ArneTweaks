#version 330

// ArneTweaks-Shader: Warm - goldenes Licht, leichter dunkler Rand

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// Dunkler Rand: 0 in der Mitte, steigt zu den Ecken
float vignette(vec2 uv, float strength) {
    vec2 d = uv - 0.5;
    return 1.0 - strength * smoothstep(0.25, 0.85, length(d * vec2(1.0, 0.8)) * 1.4);
}

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    c *= vec3(1.08, 1.0, 0.86);
    c = mix(vec3(luma(c)), c, 1.12);
    c *= vignette(texCoord, 0.25);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
