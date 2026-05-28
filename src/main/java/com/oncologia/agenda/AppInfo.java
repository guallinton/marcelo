package com.oncologia.agenda;

public final class AppInfo {
    public static final String NAME = "Agenda Quimioterapia";
    public static final String VERSION = "1.8.0";
    public static final String RELEASE_NAME = "Agenda clinica visual";

    private AppInfo() {
    }

    public static String displayName() {
        return NAME + " v" + VERSION;
    }
}
