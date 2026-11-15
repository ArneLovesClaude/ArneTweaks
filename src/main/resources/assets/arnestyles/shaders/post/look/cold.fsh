#version 330

// ArneTweaks-Shader: Kalt - kuehles, klares Bild

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

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;
    c *= vec3(0.9, 1.0, 1.12);
    c = (c - 0.5) * 1.06 + 0.5;
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
