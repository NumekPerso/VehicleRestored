#version 150

#moj_import <fog.glsl>

// Vanilla particle shader, plus the glow flag: glowing particles (fire) use the light value 248,
// which the world never gives (block light stops at 240), and still read the brightest lightmap texel.
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;
out float occlusion;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = fog_distance(ModelViewMat, Position, FogShape);
    texCoord0 = UV0;
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    occlusion = UV2.x > 244 ? 0.6 : 1.0;
}
