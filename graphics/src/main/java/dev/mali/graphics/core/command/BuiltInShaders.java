package dev.mali.graphics.core.command;

/**
 * Single-source GLSL for built-in pipelines (docs/shader-pipeline.md).
 * The GLES backend prepends the proper "#version" header for ES2/ES3.
 */
public final class BuiltInShaders {
    private BuiltInShaders() {}

    public static final String SPRITE_VS_BODY = ""
            + "uniform mat4 uMVP;\n"
            + "in vec2 aPos;\n"
            + "in vec2 aUV;\n"
            + "in vec4 aColor;\n"
            + "out vec2 vUV;\n"
            + "out vec4 vColor;\n"
            + "void main() {\n"
            + "  vUV = aUV;\n"
            + "  vColor = aColor;\n"
            + "  gl_Position = uMVP * vec4(aPos, 0.0, 1.0);\n"
            + "}\n";

    public static final String SPRITE_FS_BODY = ""
            + "precision mediump float;\n"
            + "in vec2 vUV;\n"
            + "in vec4 vColor;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform bool uUseTex;\n"
            + "out vec4 fragColor;\n"
            + "void main() {\n"
            + "  vec4 t = uUseTex ? texture(uTex, vUV) : vec4(1.0);\n"
            + "  fragColor = t * vColor;\n"
            + "}\n";

    /** Test 12 stress: deliberate ALU chain + branchy time distortion. */
    public static final String STRESS_FS_BODY = ""
            + "precision highp float;\n"
            + "in vec2 vUV;\n"
            + "in vec4 vColor;\n"
            + "uniform float uTime;\n"
            + "out vec4 fragColor;\n"
            + "void main() {\n"
            + "  vec2 p = vUV - 0.5;\n"
            + "  float d = 0.0;\n"
            + "  for (int i = 0; i < 8; i++) {\n"
            + "    p = p * 1.13 + vec2(sin(uTime * 0.7 + float(i)), cos(uTime * 0.9 + float(i)));\n"
            + "    d += length(p) * 0.02;\n"
            + "  }\n"
            + "  float m = sin(d * 40.0 + uTime) * 0.5 + 0.5;\n"
            + "  fragColor = vec4(m * vColor.rgb, vColor.a);\n"
            + "}\n";

    /** ES 1.00 variant of the sprite fragment shader (Utgard fallback). */
    public static final String SPRITE_FS_BODY_ES2 = ""
            + "precision mediump float;\n"
            + "varying vec2 vUV;\n"
            + "varying vec4 vColor;\n"
            + "uniform sampler2D uTex;\n"
            + "uniform bool uUseTex;\n"
            + "void main() {\n"
            + "  vec4 t = uUseTex ? texture2D(uTex, vUV) : vec4(1.0);\n"
            + "  gl_FragColor = t * vColor;\n"
            + "}\n";

    public static String vertexSource(boolean gles3) {
        return (gles3 ? "#version 300 es\n" : "") + SPRITE_VS_BODY
                .replaceAll("\\bin\\b", gles3 ? "in" : "attribute")
                .replaceAll("\\bout\\b", gles3 ? "out" : "varying");
    }

    public static String fragmentSource(boolean gles3) {
        if (gles3) return "#version 300 es\n" + SPRITE_FS_BODY;
        return SPRITE_FS_BODY_ES2
                .replaceAll("\\bin\\b", "varying");
    }

    public static String stressFragmentSource(boolean gles3) {
        if (gles3) return "#version 300 es\n" + STRESS_FS_BODY;
        // ES2: convert out→varying, fragColor→gl_FragColor, texture→texture2D
        return STRESS_FS_BODY
                .replaceAll("\\bin\\b", "varying")
                .replaceAll("\\bout vec4 fragColor;", "")
                .replaceAll("fragColor", "gl_FragColor")
                .replaceAll("\\btexture\\(", "texture2D(")
                .replaceAll("precision highp", "precision mediump");
    }
}
