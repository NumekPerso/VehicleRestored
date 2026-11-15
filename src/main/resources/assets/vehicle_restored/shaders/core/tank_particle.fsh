#version 150

#moj_import <fog.glsl>

// Tank effects (smoke, fire). Unlike the vanilla particle shader there is no alpha cut-off, so soft
// edges stay soft. The output is premultiplied (blend: one, one minus source alpha): smoke blends
// normally, glowing particles (occlusion below 1) add their light over what is behind them.
uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec2 texCoord0;
in vec4 vertexColor;
in float occlusion;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.003) {
        discard;
    }
    color = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    fragColor = vec4(color.rgb * color.a, color.a * occlusion);
}
