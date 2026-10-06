package dev.mali.graphics.core.mali;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mali GPU family detection from GL_RENDERER strings (docs/mali-architecture.md).
 * Pure Java — unit tested. Evidence: renderer naming is documented per family (CONFIRMED).
 */
public enum MaliFamily {
    UTGARD,     // Mali-300/400/450 — GLES2 only
    MIDGARD,    // Mali-T6xx/T7xx/T8xx — ES3.0/3.1
    BIFROST,    // Mali-G31/51/52/71/72/76 — ES3.1/3.2
    VALHALL,    // Mali-G57/68/77/78/79, G610/G710 — ES3.2
    FIFTH_GEN,  // Mali-G615/G715/G720/Immortalis-G715/G925/... — CSF
    UNKNOWN;

    private static final Pattern P = Pattern.compile(
            "(?i)mali[- ]?(immortalis[- ]?)?(g|t)?(\\d{2,3})");

    public static MaliFamily fromRenderer(String glRenderer) {
        if (glRenderer == null) return UNKNOWN;
        Matcher m = P.matcher(glRenderer);
        if (!m.find()) return UNKNOWN;
        boolean immortalis = m.group(1) != null;
        String prefix = m.group(2) == null ? "" : m.group(2).toUpperCase();
        int num = Integer.parseInt(m.group(3));

        if (prefix.equals("G")) {
            // 5th gen / Immortalis (CSF): G615/G620/G715/G720/G925/G927 + Immortalis branding
            if (immortalis || num == 615 || num == 620 || num == 715 || num == 720
                    || num == 925 || num == 927) return FIFTH_GEN;
            // Valhall: G57/G68/G77/G78/G79 (JM) + G610/G710 (CSF) — same family behavior for our runtime
            if (num == 57 || num == 68 || num == 77 || num == 78 || num == 79
                    || num == 610 || num == 710) return VALHALL;
            // Bifrost: G31/G51/G52/G71/G72/G76 + G310/G510/G520 (2020 entry/mid)
            if (num == 31 || num == 51 || num == 52 || num == 71 || num == 72 || num == 76
                    || num == 310 || num == 510 || num == 520) return BIFROST;
            // future 3-digit parts: assume latest gen (recorded as HYPOTHESIS in DB)
            if (num >= 900 || num >= 630 || num == 730) return FIFTH_GEN;
            return UNKNOWN;
        }
        if (prefix.equals("T")) return MIDGARD;            // Mali-T6xx/T7xx/T8xx
        if (prefix.isEmpty() && num <= 450) return UTGARD; // Mali-300/400/450 MP
        return UNKNOWN;
    }

    /** Highest GLES this family can expose (informational; runtime still queries driver). */
    public int maxExpectedGlesMajor() {
        switch (this) {
            case UTGARD: return 2;
            case MIDGARD: return 3;
            case BIFROST: return 3;
            case VALHALL: return 3;
            case FIFTH_GEN: return 3;
            default: return 2;
        }
    }
}
