#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

uniform sampler2D Sampler0;

float median3(vec3 value) {
    return max(min(value.r, value.g), min(max(value.r, value.g), value.b));
}

void main() {
    float signedDistance = median3(texture(Sampler0, texCoord0).rgb) - 0.5;
    float aaWidth = max(fwidth(signedDistance), 0.004);
    float alpha = clamp(signedDistance / aaWidth + 0.5, 0.0, 1.0);
    if (alpha <= 0.004) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
}
