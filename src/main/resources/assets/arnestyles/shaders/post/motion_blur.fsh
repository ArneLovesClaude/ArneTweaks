#version 330

// Motion Blur: das aktuelle Bild wird mit dem vorherigen (schon verwischten) Bild gemischt.
uniform sampler2D MainSampler;
uniform sampler2D PrevSampler;

layout(std140) uniform MotionBlurConfig {
    // Anteil des vorherigen Bildes (0 = kein Blur)
    float BlendFactor;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 current = texture(MainSampler, texCoord).rgb;
    vec4 previous = texture(PrevSampler, texCoord);
    // Frisch angelegtes Ziel ist leer (Alpha 0): dann nur das aktuelle Bild, sonst blendet es von Schwarz ein
    vec3 prev = previous.a < 0.5 ? current : previous.rgb;
    // Im linearen Farbraum mischen, damit helle Kanten nicht dunkel verschmieren
    vec3 mixed = mix(current * current, prev * prev, BlendFactor);
    fragColor = vec4(sqrt(mixed), 1.0);
}
