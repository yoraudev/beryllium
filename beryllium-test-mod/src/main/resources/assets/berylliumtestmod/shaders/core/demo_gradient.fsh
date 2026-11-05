#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    float coverage = texture(Sampler0, texCoord0).a;
    if (coverage <= 0.004) {
        discard;
    }
    vec3 top = vec3(0.1, 0.8, 1.0);
    vec3 bottom = vec3(1.0, 0.2, 0.6);
    vec3 tint = mix(top, bottom, clamp(texCoord0.y, 0.0, 1.0));
    fragColor = vec4(tint * vertexColor.rgb, coverage * vertexColor.a);
}