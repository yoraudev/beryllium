#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 fragLocal;
layout(location = 2) in vec2 fragHalfSize;
layout(location = 3) in float fragRadius;
layout(location = 4) in float fragBorderWidth;
layout(location = 5) in vec4 fragBorderColor;
layout(location = 6) in vec4 fragShadowColor;
layout(location = 7) in vec4 fragShadowParams;

layout(location = 0) out vec4 fragColor;

float roundedBoxDistance(vec2 point, vec2 boxHalfSize, float cornerRadius) {
    vec2 corner = abs(point) - boxHalfSize + vec2(cornerRadius);
    float outside = length(max(corner, vec2(0.0)));
    float inside = min(max(corner.x, corner.y), 0.0);
    return outside + inside - cornerRadius;
}

void main() {
    vec2 size = max(fragHalfSize, vec2(0.0));
    float radius = clamp(fragRadius, 0.0, min(size.x, size.y));
    float border = clamp(fragBorderWidth, 0.0, min(size.x, size.y));
    vec2 shadowOffset = fragShadowParams.xy;
    float shadowSpread = max(fragShadowParams.z, 0.0);

    float aa = max(fwidth(fragLocal.x), fwidth(fragLocal.y));

    float boxDistance = roundedBoxDistance(fragLocal, size, radius);
    float fillCoverage = clamp(0.5 - boxDistance / aa, 0.0, 1.0);

    float innerDistance = roundedBoxDistance(fragLocal, max(size - vec2(border), vec2(0.0)), max(radius - border, 0.0));
    float borderCoverage = border > 0.0 ? clamp(0.5 + innerDistance / aa, 0.0, 1.0) * fillCoverage : 0.0;

    vec4 surface = vec4(mix(vertexColor.rgb, fragBorderColor.rgb, borderCoverage), vertexColor.a * fillCoverage);

    vec4 shadow = vec4(0.0);
    if (fragShadowColor.a > 0.0 && shadowSpread > 0.0) {
        float shadowDistance = roundedBoxDistance(fragLocal - shadowOffset, size + vec2(shadowSpread), radius + shadowSpread);
        float shadowCoverage = clamp(-shadowDistance / shadowSpread, 0.0, 1.0) * clamp(0.5 + boxDistance / aa, 0.0, 1.0);
        shadow = vec4(fragShadowColor.rgb, fragShadowColor.a * shadowCoverage);
    }

    float alpha = surface.a + shadow.a * (1.0 - surface.a);
    if (alpha <= 0.0) {
        discard;
    }
    vec3 rgb = (surface.rgb * surface.a + shadow.rgb * shadow.a * (1.0 - surface.a)) / alpha;
    fragColor = vec4(rgb, alpha);
}
