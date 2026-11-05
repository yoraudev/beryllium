#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Transforms {
    mat4 ProjMat;
    mat4 ModelViewMat;
    vec2 ScreenSize;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 LocalPos;
layout(location = 3) in vec2 HalfSize;
layout(location = 4) in float Radius;
layout(location = 5) in float BorderWidth;
layout(location = 6) in vec4 BorderColor;
layout(location = 7) in vec4 ShadowColor;
layout(location = 8) in vec4 ShadowParams;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 fragLocal;
layout(location = 2) out vec2 fragHalfSize;
layout(location = 3) out float fragRadius;
layout(location = 4) out float fragBorderWidth;
layout(location = 5) out vec4 fragBorderColor;
layout(location = 6) out vec4 fragShadowColor;
layout(location = 7) out vec4 fragShadowParams;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    fragLocal = LocalPos;
    fragHalfSize = HalfSize;
    fragRadius = Radius;
    fragBorderWidth = BorderWidth;
    fragBorderColor = BorderColor;
    fragShadowColor = ShadowColor;
    fragShadowParams = ShadowParams;
}
