package dev.mali.lab.scenes;

import java.util.Random;

/**
 * Procedural test textures (checker, gradient, noise) — no bundled assets needed,
 * keeps the Lab APK tiny while stressing real upload paths (Test 05/11).
 */
final class TestTextures {
    private TestTextures() {}

    static byte[] checker(int size, int cell, int a) {
        byte[] px = new byte[size * size * 4];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean on = ((x / cell) + (y / cell)) % 2 == 0;
                int i = (y * size + x) * 4;
                px[i] = (byte) (on ? 240 : 40);
                px[i + 1] = (byte) (on ? 240 : 220);
                px[i + 2] = (byte) (on ? 240 : 120);
                px[i + 3] = (byte) a;
            }
        }
        return px;
    }

    static byte[] gradient(int size) {
        byte[] px = new byte[size * size * 4];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int i = (y * size + x) * 4;
                px[i] = (byte) (x * 255 / size);
                px[i + 1] = (byte) (y * 255 / size);
                px[i + 2] = (byte) 128;
                px[i + 3] = (byte) 255;
            }
        }
        return px;
    }

    static byte[] noise(int size, long seed) {
        Random r = new Random(seed);
        byte[] px = new byte[size * size * 4];
        r.nextBytes(px);
        for (int i = 3; i < px.length; i += 4) px[i] = (byte) 255;
        return px;
    }
}
