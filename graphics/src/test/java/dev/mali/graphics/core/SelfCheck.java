package dev.mali.graphics.core;

/**
 * Sandbox-runnable verification of the pure-Java core (no Android, no JUnit).
 * Same assertions as Tests.runAll(); used by docs/PERFORMANCE_REPORT.md.
 */
public final class SelfCheck {
    public static void main(String[] args) {
        Tests.runAll();
        System.out.println("[SelfCheck] Mali V3 core: ALL GREEN");
    }
}
