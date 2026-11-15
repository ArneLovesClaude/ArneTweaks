#version 330

// ArneTweaks-Shader: Gluehen - helle Stellen strahlen weich nach aussen

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
    vec2 px = 1.0 / InSize;
    vec3 c = texture(InSampler, texCoord).rgb;
    vec3 glow = vec3(0.0);
    float total = 0.0;
    // Ring aus 16 Proben in zwei Abstaenden, nur helle Stellen zaehlen
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.785398;
        vec2 dir = vec2(cos(a), sin(a));
        for (int r = 1; r <= 2; r++) {
            vec3 s = texture(InSampler, texCoord + dir * px * 6.0 * float(r)).rgb;
            glow += s * smoothstep(0.65, 1.0, luma(s));
            total += 1.0;
        }
    }
    c += glow / total * 0.9;
    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
