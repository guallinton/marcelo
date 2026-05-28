window.AgendaConfig = {
  version: "1.0.0-web",
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
    { id: "FOLFOX", label: "FOLFOX", duration: 180, color: "#1976d2" },
    { id: "FOLFIRI", label: "FOLFIRI", duration: 180, color: "#7b1fa2" },
    { id: "AC_T", label: "AC-T", duration: 120, color: "#c2185b" },
    { id: "CARBO_PACL", label: "Carboplatino + Paclitaxel", duration: 240, color: "#00897b" },
    { id: "RITUXIMAB", label: "Rituximab", duration: 210, color: "#ef6c00" },
    { id: "INMUNO", label: "Inmunoterapia", duration: 90, color: "#455a64" },
    { id: "OTRO", label: "Otro", duration: 60, color: "#5d6b82" }
  ]
};
