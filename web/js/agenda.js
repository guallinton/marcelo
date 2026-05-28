window.AgendaView = (function () {
  const cfg = window.AgendaConfig;
  const DAY_NAMES = ["Dom", "Lun", "Mar", "Mie", "Jue", "Vie", "Sab"];
  const MONTH_NAMES = [
    "ene", "feb", "mar", "abr", "may", "jun",
    "jul", "ago", "sep", "oct", "nov", "dic"
  ];

  function slotHeightPx() {
    return parseFloat(
      getComputedStyle(document.documentElement).getPropertyValue("--slot-h") || "56"
    );
  }

  function parseTime(hhmm) {
    const [h, m] = hhmm.split(":").map(Number);
    return { h, m, total: h * 60 + m };
  }

  function formatTime(date) {
    const h = String(date.getHours()).padStart(2, "0");
    const m = String(date.getMinutes()).padStart(2, "0");
    return `${h}:${m}`;
  }

  function buildSlots() {
    const start = parseTime(cfg.workStart);
    const end = parseTime(cfg.workEnd);
    const slots = [];
    for (let t = start.total; t < end.total; t += cfg.slotMinutes) {
      const h = Math.floor(t / 60);
      const m = t % 60;
      slots.push({
        label: `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`,
        minutes: t
      });
    }
    return slots;
  }

  function doctorColor(doctorId, doctors) {
    const idx = doctors.findIndex((d) => d.id === doctorId);
    const palette = cfg.doctorColors;
    return palette[(idx >= 0 ? idx : 0) % palette.length];
  }

  function enrichAppointments(appointments, context) {
    const { patients, doctors, overlaps } = context;
    return appointments.map((appt) => {
      const patient = patients.find((p) => p.id === appt.patientId);
      const doctor = doctors.find((d) => d.id === appt.doctorId);
      const protocol =
        cfg.protocols.find((p) => p.id === (patient && patient.protocolId)) || cfg.protocols[6];
      return {
        ...appt,
        patient,
        doctor,
        protocol,
        conflict: overlaps.has(appt.id),
        startDate: new Date(appt.start),
        endDate: AgendaStorage.appointmentEnd(appt),
        endCleanDate: AgendaStorage.appointmentEndWithCleaning(appt)
      };
    });
  }

  function computeOverlapIds(appointments) {
    const ids = new Set();
    for (let i = 0; i < appointments.length; i++) {
      for (let j = i + 1; j < appointments.length; j++) {
        const a = appointments[i];
        const b = appointments[j];
        if (a.bedChair !== b.bedChair) {
          continue;
        }
        const aStart = new Date(a.start);
        const aEnd = AgendaStorage.appointmentEndWithCleaning(a);
        const bStart = new Date(b.start);
        const bEnd = AgendaStorage.appointmentEndWithCleaning(b);
        if (aStart < bEnd && aEnd > bStart) {
          ids.add(a.id);
          ids.add(b.id);
        }
      }
    }
    return ids;
  }

  function columnDefs(view, baseDate) {
    if (view === "week") {
      const cols = [];
      for (let i = 0; i < 7; i++) {
        const date = AgendaStorage.addDays(baseDate, i);
        cols.push({
          key: AgendaStorage.toIsoDate(date),
          date,
          label: DAY_NAMES[date.getDay()],
          sub: `${date.getDate()} ${MONTH_NAMES[date.getMonth()]}`
        });
      }
      return cols;
    }

    const cols = [];
    for (let bed = 1; bed <= cfg.bedCount; bed++) {
      cols.push({
        key: `bed-${bed}`,
        date: baseDate,
        bed,
        label: `Cama ${bed}`,
        sub: formatDayHeader(baseDate)
      });
    }
    return cols;
  }

  function formatDayHeader(date) {
    return `${DAY_NAMES[date.getDay()]} ${date.getDate()} ${MONTH_NAMES[date.getMonth()]}`;
  }

  function formatRange(view, baseDate) {
    if (view === "week") {
      const end = AgendaStorage.addDays(baseDate, 6);
      return `${baseDate.getDate()} ${MONTH_NAMES[baseDate.getMonth()]} – ${end.getDate()} ${MONTH_NAMES[end.getMonth()]} ${end.getFullYear()}`;
    }
    return formatDayHeader(baseDate) + " " + baseDate.getFullYear();
  }

  function appointmentsForColumn(apptList, col, view) {
    if (view === "week") {
      return apptList.filter((a) => a.start.slice(0, 10) === col.key);
    }
    return apptList.filter(
      (a) =>
        a.start.slice(0, 10) === AgendaStorage.toIsoDate(col.date) && a.bedChair === col.bed
    );
  }

  function topOffset(startDate, slots, slotH) {
    const minutes = startDate.getHours() * 60 + startDate.getMinutes();
    const first = slots[0].minutes;
    return ((minutes - first) / cfg.slotMinutes) * slotH;
  }

  function cardHeight(durationMinutes, slotH) {
    return (durationMinutes / cfg.slotMinutes) * slotH;
  }

  function render(options) {
    const {
      root,
      view,
      baseDate,
      appointments,
      patients,
      doctors,
      doctorFilterId,
      selectedId,
      canEdit,
      onSlotClick,
      onSelect
    } = options;

    const slots = buildSlots();
    const cols = columnDefs(view, baseDate);
    const slotH = slotHeightPx();
    const overlaps = computeOverlapIds(appointments);
    let enriched = enrichAppointments(appointments, { patients, doctors, overlaps });

    if (doctorFilterId) {
      enriched = enriched.filter((a) => a.doctorId === doctorFilterId);
    }

    root.style.setProperty("--cols", String(cols.length));
    root.style.setProperty("--rows", String(slots.length));
    root.innerHTML = "";

    const grid = document.createElement("div");
    grid.className = "agenda-grid";
    grid.setAttribute("role", "grid");

    const corner = document.createElement("div");
    corner.className = "agenda-corner";
    corner.textContent = view === "week" ? "Semana" : "Dia";
    grid.appendChild(corner);

    cols.forEach((col) => {
      const head = document.createElement("div");
      head.className = "agenda-day-header";
      head.innerHTML = `${escapeHtml(col.label)}<small>${escapeHtml(col.sub)}</small>`;
      grid.appendChild(head);
    });

    const timeRail = document.createElement("div");
    timeRail.className = "agenda-time-rail";
    timeRail.style.height = `${slots.length * slotH}px`;
    slots.forEach((slot, index) => {
      const label = document.createElement("div");
      label.className = "agenda-time";
      label.style.height = `${slotH}px`;
      label.textContent = slot.label;
      timeRail.appendChild(label);
    });
    grid.appendChild(timeRail);

    cols.forEach((col) => {
      const column = document.createElement("div");
      column.className = "agenda-column";
      column.dataset.colKey = col.key;

      const colAppts = appointmentsForColumn(enriched, col, view);
      colAppts.forEach((appt) => {
        const card = document.createElement("button");
        card.type = "button";
        card.className = "appointment-card";
        if (appt.id === selectedId) {
          card.classList.add("is-selected");
        }
        if (appt.conflict) {
          card.classList.add("is-conflict");
        }
        card.style.setProperty("--doctor-color", doctorColor(appt.doctorId, doctors));
        card.style.top = `${topOffset(appt.startDate, slots, slotH)}px`;
        card.style.height = `${Math.max(cardHeight(appt.durationMinutes, slotH) - 4, 44)}px`;
        card.dataset.id = String(appt.id);

        const patientName = appt.patient
          ? `${appt.patient.firstName} ${appt.patient.lastName}`
          : "Paciente";
        const doctorName = appt.doctor ? appt.doctor.fullName : "";
        card.innerHTML =
          `<p class="appointment-card__patient">${escapeHtml(patientName)}</p>` +
          `<p class="appointment-card__time">${escapeHtml(formatTime(appt.startDate))} – ${escapeHtml(formatTime(appt.endDate))}</p>` +
          `<p class="appointment-card__meta">${escapeHtml(appt.protocol.label)} · ${escapeHtml(doctorName)} · Cama ${appt.bedChair}</p>` +
          (appt.conflict ? '<span class="appointment-card__badge">Conflicto</span>' : "");

        card.addEventListener("click", (e) => {
          e.stopPropagation();
          onSelect(appt.id);
        });
        column.appendChild(card);
      });

      slots.forEach((slot, index) => {
        const hit = document.createElement("button");
        hit.type = "button";
        hit.className = "agenda-slot-hit";
        hit.style.top = `${index * slotH}px`;
        hit.title = `Crear turno ${col.label} ${slot.label}`;
        hit.addEventListener("click", () => {
          if (!canEdit) {
            return;
          }
          const dateIso = view === "week" ? col.key : AgendaStorage.toIsoDate(col.date);
          const bed = view === "day" ? col.bed : 1;
          onSlotClick({ dateIso, time: slot.label, bed });
        });
        column.appendChild(hit);
      });

      grid.appendChild(column);
    });

    root.appendChild(grid);

    return {
      metrics: buildMetrics(enriched, appointments, cols, slots)
    };
  }

  function buildMetrics(visible, allInRange, cols, slots) {
    let occupied = 0;
    const seen = new Set();
    visible.forEach((a) => {
      const slotsUsed = Math.ceil(a.durationMinutes / cfg.slotMinutes);
      const key = `${a.start}-${a.bedChair}`;
      if (!seen.has(key)) {
        seen.add(key);
        occupied += slotsUsed;
      }
    });
    const conflicts = computeOverlapIds(allInRange).size;
    return {
      totalSlots: cols.length * slots.length,
      occupied,
      appointmentCount: visible.length,
      conflictCount: conflicts
    };
  }

  function escapeHtml(text) {
    return String(text || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function countByDoctor(appointments, doctorId) {
    return appointments.filter((a) => a.doctorId === doctorId).length;
  }

  return {
    render,
    buildSlots,
    formatRange,
    formatDayHeader,
    doctorColor,
    countByDoctor,
    escapeHtml
  };
})();
