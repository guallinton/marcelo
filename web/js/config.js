window.AgendaConfig = {
  version: "1.9.0-web",
  appName: "Agenda Quimioterapia Web",
  storageKey: "agenda_oncologia_web_v1",
  workStart: "08:00",
  workEnd: "18:00",
  slotMinutes: 30,
  bedCount: 4,
  cleaningMinutes: 15,
  doctorColors: [
    "#0eb5e1",
    "#7c6fe0",
    "#ff617a",
    "#22c55e",
    "#f59e0b",
    "#8b5cf6"
  ],
  protocols: [
    { id: "FOLFOX", label: "FOLFOX", guideline: "NCCN Colon / ESMO mCRC — FOLFOX6", infusionMinutes: 150, duration: 180, color: "#1976d2" },
    { id: "FOLFIRI", label: "FOLFIRI", guideline: "NCCN Colon / ESMO mCRC", infusionMinutes: 150, duration: 180, color: "#7b1fa2" },
    { id: "AC_T", label: "AC-T", guideline: "NCCN Breast / ESMO — AC", infusionMinutes: 90, duration: 120, color: "#c2185b" },
    { id: "CARBO_PACL", label: "Carboplatino + Paclitaxel", guideline: "NCCN Ovarian / Lung", infusionMinutes: 210, duration: 240, color: "#00897b" },
    { id: "RITUXIMAB", label: "Rituximab", guideline: "ESMO Lymphoma / NCCN B-cell", infusionMinutes: 180, duration: 210, color: "#ef6c00" },
    { id: "INMUNO", label: "Inmunoterapia", guideline: "NCCN / ESMO anti-PD-1/PD-L1", infusionMinutes: 60, duration: 90, color: "#455a64" },
    { id: "DOCETAXEL", label: "Docetaxel", guideline: "NCCN Breast / Prostate", infusionMinutes: 60, duration: 75, color: "#5c6bc0" },
    { id: "BEVACIZUMAB", label: "Bevacizumab", guideline: "NCCN Colon / ESMO mCRC", infusionMinutes: 60, duration: 90, color: "#00695c" },
    { id: "OTRO", label: "Otro", guideline: "Personalizado", infusionMinutes: 45, duration: 60, color: "#5d6b82" }
  ]
};
