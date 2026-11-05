#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform Transforms {
    mat4 ProjMat;
    mat4 ModelViewMat;
    vec2 ScreenSize;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(location = 0) out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
}
