#version 330

// ArneTweaks-Shader: Kino - Schatten blaugruen, Licht orange, Kontrast, dunkler Rand

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
    float l = luma(c);
    vec3 shadows = vec3(0.0, 0.05, 0.08);
    vec3 highlights = vec3(0.08, 0.03, -0.04);
    c += mix(shadows, highlights, smoothstep(0.2, 0.8, l));
    c = (c - 0.5) * 1.15 + 0.5;
    c *= vignette(texCoord, 0.4);
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
