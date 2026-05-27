package com.oncologia.agenda.model;

public enum ChemoProtocol {
    FOLFOX("FOLFOX", 180, "#1976d2"),
    FOLFIRI("FOLFIRI", 180, "#7b1fa2"),
    AC_T("AC-T", 120, "#c2185b"),
    CARBOPLATINO_PACLITAXEL("Carboplatino + Paclitaxel", 240, "#00897b"),
    RITUXIMAB("Rituximab", 210, "#ef6c00"),
    INMUNOTERAPIA("Inmunoterapia", 90, "#455a64"),
    OTRO("Otro", 60, "#5d6b82");

    private final String label;
    private final int defaultDurationMinutes;
    private final String color;

    ChemoProtocol(String label, int defaultDurationMinutes, String color) {
        this.label = label;
        this.defaultDurationMinutes = defaultDurationMinutes;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public int getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return label;
    }
}
