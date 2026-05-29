/**
 * Agenda con FullCalendar (https://github.com/fullcalendar/fullcalendar)
 * Vistas timeGridWeek / timeGridDay alineadas al horario de infusion.
 */
window.AgendaView = (function () {
  const cfg = window.AgendaConfig;
  let calendar = null;
  let calendarEl = null;
  let lastCallbacks = {};

  function parseTime(hhmm) {
    const [h, m] = hhmm.split(":").map(Number);
    return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}:00`;
  }

  function toLocalDateTime(date) {
    const y = date.getFullYear();
    const mo = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    const h = String(date.getHours()).padStart(2, "0");
    const mi = String(date.getMinutes()).padStart(2, "0");
    return `${y}-${mo}-${d}T${h}:${mi}:00`;
  }

  function doctorColor(doctorId, doctors) {
    const idx = doctors.findIndex((d) => d.id === doctorId);
    return cfg.doctorColors[(idx >= 0 ? idx : 0) % cfg.doctorColors.length];
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

  function toFullCalendarEvents(appointments, patients, doctors, doctorFilterId, selectedId) {
    const overlaps = computeOverlapIds(appointments);
    return appointments
      .filter((a) => !doctorFilterId || a.doctorId === doctorFilterId)
      .map((appt) => {
        const patient = patients.find((p) => p.id === appt.patientId);
        const doctor = doctors.find((d) => d.id === appt.doctorId);
        const protocol =
          cfg.protocols.find((p) => p.id === (patient && patient.protocolId)) || cfg.protocols[8];
        const patientName = patient
          ? `${patient.firstName} ${patient.lastName}`
          : "Paciente";
        const end = AgendaStorage.appointmentEnd(appt);
        const color = doctorColor(appt.doctorId, doctors);
        const conflict = overlaps.has(appt.id);

        return {
          id: String(appt.id),
          title: patientName,
          start: appt.start,
          end: toLocalDateTime(end),
          backgroundColor: conflict ? "#fecdd3" : color,
          borderColor: conflict ? "#ef4444" : color,
          textColor: conflict ? "#7f1d1d" : "#1a2b3c",
          classNames: [
            conflict ? "fc-event-conflict" : "",
            Number(selectedId) === appt.id ? "fc-event-selected" : ""
          ].filter(Boolean),
          extendedProps: {
            bed: appt.bedChair,
            doctorName: doctor ? doctor.fullName : "",
            protocolLabel: protocol.label,
            durationMinutes: appt.durationMinutes,
            conflict
          }
        };
      });
  }

  function buildMetrics(events, allAppointments) {
    const conflicts = computeOverlapIds(allAppointments).size;
    return {
      appointmentCount: events.length,
      occupied: events.reduce(
        (sum, e) => sum + Math.ceil((e.extendedProps.durationMinutes || 30) / cfg.slotMinutes),
        0
      ),
      conflictCount: conflicts
    };
  }

  function renderEventContent(arg) {
    const p = arg.event.extendedProps;
    const time = arg.timeText ? `<div class="fc-event-time-custom">${escapeHtml(arg.timeText)}</div>` : "";
    return {
      html:
        `<div class="fc-custom-event">` +
        time +
        `<div class="fc-custom-event__title">${escapeHtml(arg.event.title)}</div>` +
        `<div class="fc-custom-event__meta">${escapeHtml(p.protocolLabel)} · Cama ${p.bed}</div>` +
        `<div class="fc-custom-event__doctor">${escapeHtml(p.doctorName)}</div>` +
        (p.conflict ? '<span class="fc-custom-event__badge">Conflicto</span>' : "") +
        `</div>`
    };
  }

  function createCalendar(rootEl, options, callbacks) {
    lastCallbacks = callbacks;
    calendarEl = rootEl;
    const viewName = options.view === "week" ? "timeGridWeek" : "timeGridDay";

    calendar = new FullCalendar.Calendar(calendarEl, {
      locale: "es",
      firstDay: 1,
      initialView: viewName,
      initialDate: options.baseDate,
      headerToolbar: false,
      height: "100%",
      expandRows: true,
      allDaySlot: false,
      nowIndicator: true,
      slotMinTime: parseTime(cfg.workStart),
      slotMaxTime: parseTime(cfg.workEnd),
      slotDuration: `00:${String(cfg.slotMinutes).padStart(2, "0")}:00`,
      slotLabelInterval: `00:${String(cfg.slotMinutes).padStart(2, "0")}:00`,
      slotLabelFormat: { hour: "2-digit", minute: "2-digit", hour12: false },
      eventTimeFormat: { hour: "2-digit", minute: "2-digit", hour12: false },
      dayHeaderFormat: { weekday: "short", day: "numeric", month: "short" },
      weekends: true,
      selectable: !!options.canEdit,
      selectMirror: true,
      unselectAuto: true,
      events: toFullCalendarEvents(
        options.appointments,
        options.patients,
        options.doctors,
        options.doctorFilterId,
        options.selectedId
      ),
      eventContent: renderEventContent,
      dateClick(info) {
        if (!options.canEdit) {
          return;
        }
        const d = info.date;
        const dateIso = AgendaStorage.toIsoDate(d);
        const time = `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
        callbacks.onSlotClick({ dateIso, time, bed: 1 });
      },
      select(info) {
        if (!options.canEdit) {
          return;
        }
        const d = info.start;
        const dateIso = AgendaStorage.toIsoDate(d);
        const time = `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
        calendar.unselect();
        callbacks.onSlotClick({ dateIso, time, bed: 1 });
      },
      eventClick(info) {
        info.jsEvent.preventDefault();
        callbacks.onSelect(Number(info.event.id));
      },
      datesSet() {
        if (callbacks.onDatesChange) {
          callbacks.onDatesChange(calendar.getDate(), calendar.view.title);
        }
      }
    });

    calendar.render();
    return buildMetrics(
      calendar.getEvents().map((e) => ({
        extendedProps: e.extendedProps
      })),
      options.appointments
    );
  }

  function updateCalendar(options, callbacks) {
    lastCallbacks = callbacks;
    const viewName = options.view === "week" ? "timeGridWeek" : "timeGridDay";
    if (calendar.view.type !== viewName) {
      calendar.changeView(viewName);
    }
    calendar.gotoDate(options.baseDate);
    calendar.removeAllEvents();
    calendar.addEventSource(
      toFullCalendarEvents(
        options.appointments,
        options.patients,
        options.doctors,
        options.doctorFilterId,
        options.selectedId
      )
    );
    calendar.setOption("selectable", !!options.canEdit);
    return buildMetrics(
      calendar.getEvents().map((e) => ({ extendedProps: e.extendedProps })),
      options.appointments
    );
  }

  function render(options) {
    const callbacks = {
      onSlotClick: options.onSlotClick,
      onSelect: options.onSelect,
      onDatesChange: options.onDatesChange
    };

    if (!calendar) {
      options.root.innerHTML = '<div id="calendar" class="fc-theme-agenda"></div>';
      const el = options.root.querySelector("#calendar");
      return createCalendar(el, options, callbacks);
    }
    return updateCalendar(options, callbacks);
  }

  function formatRange(view, baseDate) {
    if (calendar && calendar.view) {
      return calendar.view.title;
    }
    const DAY_NAMES = ["Dom", "Lun", "Mar", "Mie", "Jue", "Vie", "Sab"];
    const MONTH_NAMES = [
      "ene", "feb", "mar", "abr", "may", "jun",
      "jul", "ago", "sep", "oct", "nov", "dic"
    ];
    if (view === "week") {
      const end = AgendaStorage.addDays(baseDate, 6);
      return `${baseDate.getDate()} ${MONTH_NAMES[baseDate.getMonth()]} – ${end.getDate()} ${MONTH_NAMES[end.getMonth()]} ${end.getFullYear()}`;
    }
    return `${DAY_NAMES[baseDate.getDay()]} ${baseDate.getDate()} ${MONTH_NAMES[baseDate.getMonth()]} ${baseDate.getFullYear()}`;
  }

  function countByDoctor(appointments, doctorId) {
    return appointments.filter((a) => a.doctorId === doctorId).length;
  }

  function escapeHtml(text) {
    return String(text || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function navigatePrev() {
    if (calendar) {
      calendar.prev();
    }
    return calendar ? calendar.getDate() : null;
  }

  function navigateNext() {
    if (calendar) {
      calendar.next();
    }
    return calendar ? calendar.getDate() : null;
  }

  function navigateToday() {
    if (calendar) {
      calendar.today();
    }
    return calendar ? calendar.getDate() : null;
  }

  function gotoDate(date) {
    if (calendar) {
      calendar.gotoDate(date);
    }
  }

  function getViewTitle() {
    return calendar && calendar.view ? calendar.view.title : "";
  }

  function getCalendarDate() {
    return calendar ? calendar.getDate() : null;
  }

  function destroy() {
    if (calendar) {
      calendar.destroy();
      calendar = null;
    }
  }

  return {
    render,
    formatRange,
    countByDoctor,
    escapeHtml,
    navigatePrev,
    navigateNext,
    navigateToday,
    gotoDate,
    getViewTitle,
    getCalendarDate,
    destroy
  };
})();
