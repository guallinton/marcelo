package com.oncologia.agenda.model;

/**
 * Protocolos de quimioterapia con referencia a pautas internacionales (NCCN / ESMO / ASCO)
 * y tiempos estimados de infusion y ocupacion de butaca.
 */
public enum ChemoProtocol {
    FOLFOX(
            "FOLFOX",
            "NCCN Colon Cancer / ESMO mCRC — FOLFOX6",
            150,
            180,
            "#1976d2"
    ),
    FOLFIRI(
            "FOLFIRI",
            "NCCN Colon Cancer / ESMO mCRC — FOLFIRI",
            150,
            180,
            "#7b1fa2"
    ),
    AC_T(
            "AC-T",
            "NCCN Breast Cancer / ESMO early breast — esquema AC",
            90,
            120,
            "#c2185b"
    ),
    CARBOPLATINO_PACLITAXEL(
            "Carboplatino + Paclitaxel",
            "NCCN Ovarian / Lung / ESMO — carboplatino AUC + paclitaxel",
            210,
            240,
            "#00897b"
    ),
    RITUXIMAB(
            "Rituximab",
            "ESMO Lymphoma / NCCN B-cell — rituximab IV",
            180,
            210,
            "#ef6c00"
    ),
    INMUNOTERAPIA(
            "Inmunoterapia (anti-PD-1/PD-L1)",
            "NCCN / ESMO — pembrolizumab, nivolumab u otros (1a infusion)",
            60,
            90,
            "#455a64"
    ),
    DOCETAXEL(
            "Docetaxel",
            "NCCN Breast / Prostate / ESMO — docetaxel 3 semanal",
            60,
            75,
            "#5c6bc0"
    ),
    BEVACIZUMAB(
            "Bevacizumab",
            "NCCN Colon / ESMO mCRC — bevacizumab mantenimiento",
            60,
            90,
            "#00695c"
    ),
    OTRO(
            "Otro / personalizado",
            "Sin pauta estandar — definir con equipo oncologico",
            45,
            60,
            "#5d6b82"
    );

    private final String label;
    private final String guidelineReference;
    private final int infusionMinutes;
    private final int chairOccupancyMinutes;
    private final String color;

    ChemoProtocol(String label, String guidelineReference, int infusionMinutes, int chairOccupancyMinutes, String color) {
        this.label = label;
        this.guidelineReference = guidelineReference;
        this.infusionMinutes = infusionMinutes;
        this.chairOccupancyMinutes = chairOccupancyMinutes;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getGuidelineReference() {
        return guidelineReference;
    }

    /** Tiempo estimado de infusion activa (min). */
    public int getInfusionMinutes() {
        return infusionMinutes;
    }

    /** Ocupacion probable de butaca: premedicacion + infusion + observacion (min). */
    public int getChairOccupancyMinutes() {
        return chairOccupancyMinutes;
    }

    /** Usado al agendar turnos (duracion sugerida del turno). */
    public int getDefaultDurationMinutes() {
        return chairOccupancyMinutes;
    }

    public String getColor() {
        return color;
    }

    public String getOccupancySummary() {
        return "Infusion estimada: " + infusionMinutes + " min · Ocupacion butaca probable: " + chairOccupancyMinutes + " min";
    }

    @Override
    public String toString() {
        return label;
    }
}
