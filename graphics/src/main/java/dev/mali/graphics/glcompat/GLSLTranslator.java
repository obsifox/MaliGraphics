package dev.mali.graphics.glcompat;

import java.util.HashMap;
import java.util.Map;

/**
 * Desktop GLSL -> GLSL ES (1.00) source translation for the compat layer.
 *
 * Rules (evidence-tagged in docs/gl-translation-layer.md):
 *  - strip desktop #version
 *  - fragment shaders missing precision -> prepend "precision mediump float;"
 *  - GLSL 130+ in/out -> attribute/varying (by shader stage)
 *  - texture( -> texture2D
 *  - desktop fixed-function built-ins -> our FF attrib/uniform defines
 *  - fragment "out vec4 name;" -> #define name gl_FragColor
 */
public final class GLSLTranslator {

    private GLSLTranslator() {}

    /** Pending sources keyed by real GLES shader id (set by GL20.glShaderSource). */
    public static final Map<Integer, String> pendingSources = new HashMap<>();

    public static String toEs(int shaderType, String src) {
        boolean fragment = shaderType == android.opengl.GLES20.GL_FRAGMENT_SHADER;
        StringBuilder out = new StringBuilder(src.length() + 256);

        // compat built-in defines (only for shaders that use them)
        boolean usesFFBuiltin = src.contains("gl_Vertex") || src.contains("gl_Color")
                || src.contains("gl_MultiTexCoord0") || src.contains("gl_MultiTexCoord1")
                || src.contains("gl_ModelViewProjectionMatrix") || src.contains("gl_ModelViewMatrix");
        if (usesFFBuiltin) {
            out.append("#ifdef GL_ES\n#endif\n");
            out.append("#define gl_Vertex vec4(aPos,1.0)\n");
            out.append("#define gl_Color aColor\n");
            out.append("#define gl_MultiTexCoord0 vec4(aUV0,0.0,1.0)\n");
            out.append("#define gl_MultiTexCoord1 vec4(aUV1,0.0,1.0)\n");
            out.append("#define gl_ModelViewProjectionMatrix uMVP\n");
            out.append("#define gl_ModelViewMatrix uMV\n");
            out.append("#define gl_NormalMatrix mat3(1.0)\n");
        }

        String fragOutName = null;

        for (String rawLine : src.split("\n")) {
            String line = rawLine;
            String trimmed = line.trim();

            if (trimmed.startsWith("#version")) continue;           // desktop version -> drop
            if (trimmed.startsWith("#extension")) {
                // ARB/EXT desktop extensions are meaningless on ES; drop to be safe
                if (trimmed.contains("ARB") || trimmed.contains("EXT") || trimmed.contains("_")) continue;
            }
            // GLSL 130+ stage IO
            if (!fragment && trimmed.startsWith("in "))  line = line.replaceFirst("\\bin\\b", "attribute");
            if (!fragment && trimmed.startsWith("out ")) line = line.replaceFirst("\\bout\\b", "varying");
            if (fragment && trimmed.startsWith("in "))   line = line.replaceFirst("\\bin\\b", "varying");

            // fragment out declaration -> gl_FragColor define
            if (fragment && trimmed.startsWith("out ")) {
                java.util.regex.Matcher m =
                        java.util.regex.Pattern.compile("out\\s+vec4\\s+(\\w+)\\s*;").matcher(trimmed);
                if (m.find()) {
                    out.append("#define ").append(m.group(1)).append(" gl_FragColor\n");
                    continue;
                }
            }

            line = line.replace("texture(", "texture2D(");
            out.append(line).append('\n');
        }

        String result = out.toString();
        if (fragment && !result.contains("precision")) {
            result = "precision mediump float;\n" + result;
        }
        return result;
    }
}
