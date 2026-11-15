#version 330

// ArneTweaks-Shader: Scharf - Kanten werden nachgeschaerft

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 px = 1.0 / InSize;
    vec3 c = texture(InSampler, texCoord).rgb;
    vec3 n = texture(InSampler, texCoord + vec2(0.0, px.y)).rgb
           + texture(InSampler, texCoord - vec2(0.0, px.y)).rgb
           + texture(InSampler, texCoord + vec2(px.x, 0.0)).rgb
           + texture(InSampler, texCoord - vec2(px.x, 0.0)).rgb;
    vec3 sharp = c * 1.6 - n * 0.15;
    fragColor = vec4(clamp(sharp, 0.0, 1.0), 1.0);
}
