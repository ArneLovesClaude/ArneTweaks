#version 330

// ArneTweaks-Shader: Noir - Schwarz-Weiss mit starkem Kontrast

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
    float l = luma(texture(InSampler, texCoord).rgb);
    l = smoothstep(0.05, 0.95, l);
    l *= vignette(texCoord, 0.35);
    fragColor = vec4(vec3(l), 1.0);
}
