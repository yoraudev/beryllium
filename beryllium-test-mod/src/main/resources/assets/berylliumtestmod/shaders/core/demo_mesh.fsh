#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform TintBlock {
    vec4 Tint;
};

layout(location = 0) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Mask;

void main() {
    vec4 mask = texture(Mask, texCoord0);
    fragColor = vec4(Tint.rgb * mask.rgb, Tint.a * mask.a);
}