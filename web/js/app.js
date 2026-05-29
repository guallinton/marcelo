(function () {
  const cfg = window.AgendaConfig;
  const storage = window.AgendaStorage;
  const viewApi = window.AgendaView;

  const state = {
    user: null,
    screen: "list",
    view: "week",
    baseDate: startOfWeek(new Date()),
    selectedDoctorId: null,
    selectedAppointmentId: null,
    editingId: null
  };

  const $ = (id) => document.getElementById(id);

  const loginScreen = $("login-screen");
  const appScreen = $("app-screen");
  const loginForm = $("login-form");
  const loginError = $("login-error");
  const agendaRoot = $("agenda-root");
  const turnosListPanel = $("turnos-list-panel");
  const doctorList = $("doctor-list");
  const detailContent = $("detail-content");
  const agendaMetrics = $("agenda-metrics");
  const agendaTitle = $("agenda-title");
  const agendaRange = $("agenda-range");
  const baseDateInput = $("base-date");
  const appointmentDialog = $("appointment-dialog");
  const appointmentForm = $("appointment-form");
  const apptWarning = $("appt-warning");
  const detailPanel = document.querySelector(".detail-panel");

  function startOfWeek(date) {
    return storage.startOfWeek(date);
  }

  function toIsoDate(date) {
    return storage.toIsoDate(date);
  }

  function parseInputDate(value) {
    if (!value) {
      return new Date();
    }
    const [y, m, d] = value.split("-").map(Number);
    return new Date(y, m - 1, d);
  }

  function roleLabel(role) {
    return role === "ENFERMERIA" ? "Enfermeria" : "Medico";
  }

  function canWrite() {
    return !!state.user;
  }

  function canDelete() {
    return state.user && state.user.role === "ENFERMERIA";
  }

  function canEditAppointment(appt) {
    if (!state.user || !appt) {
      return false;
    }
    if (state.user.role === "ENFERMERIA") {
      return true;
    }
    return appt.createdById === state.user.id;
  }

  function showLogin() {
    loginScreen.hidden = false;
    appScreen.hidden = true;
    state.user = null;
  }

  function showApp() {
    loginScreen.hidden = true;
    appScreen.hidden = false;
    state.screen = "list";
    state.selectedAppointmentId = null;
    $("session-user").textContent = state.user.fullName;
    const badge = $("session-role");
    badge.textContent = roleLabel(state.user.role);
    badge.className = "badge " + (state.user.role === "ENFERMERIA" ? "badge--nurse" : "badge--doctor");
    baseDateInput.value = toIsoDate(state.baseDate);
    syncScreenTabs();
    refresh();
  }

  function syncScreenTabs() {
    document.querySelectorAll("[data-screen]").forEach((btn) => {
      const active =
        btn.dataset.screen === state.screen ||
        (state.screen === "calendar" && btn.dataset.screen === "calendar");
      btn.classList.toggle("is-active", active);
    });
    turnosListPanel.hidden = state.screen !== "list";
    agendaRoot.hidden = state.screen !== "calendar";
  }

  function rangeForView() {
    if (state.view === "week") {
      const from = toIsoDate(state.baseDate);
      const to = toIsoDate(storage.addDays(state.baseDate, 6));
      return { from, to };
    }
    const day = toIsoDate(state.baseDate);
    return { from: day, to: day };
  }

  function loadAppointments() {
    const { from, to } = rangeForView();
    return storage.getAppointmentsBetween(from, to);
  }

  function syncBaseDateFromCalendar() {
    const d = viewApi.getCalendarDate();
    if (!d) {
      return;
    }
    state.baseDate = state.view === "week" ? startOfWeek(d) : new Date(d.getFullYear(), d.getMonth(), d.getDate());
    baseDateInput.value = toIsoDate(state.baseDate);
    const title = viewApi.getViewTitle();
    if (title) {
      agendaRange.textContent = title;
    }
  }

  function refresh() {
    renderDoctors();
    if (state.screen === "list") {
      renderTurnosList();
    } else {
      renderAgenda();
    }
    renderDetail();
    updateToolbar();
  }

  function computeConflicts(appointments) {
    const ids = new Set();
    for (let i = 0; i < appointments.length; i++) {
      for (let j = i + 1; j < appointments.length; j++) {
        const a = appointments[i];
        const b = appointments[j];
        if (a.bedChair !== b.bedChair) continue;
        const aStart = new Date(a.start);
        const aEnd = storage.appointmentEndWithCleaning(a);
        const bStart = new Date(b.start);
        const bEnd = storage.appointmentEndWithCleaning(b);
        if (aStart < bEnd && aEnd > bStart) {
          ids.add(a.id);
          ids.add(b.id);
        }
      }
    }
    return ids;
  }

  function renderTurnosList() {
    const appointments = loadAppointments()
      .filter((a) => !state.selectedDoctorId || a.doctorId === state.selectedDoctorId)
      .sort((a, b) => new Date(a.start) - new Date(b.start));
    const patients = storage.getPatients();
    const doctors = storage.getDoctors();
    const conflicts = computeConflicts(loadAppointments());

    if (!appointments.length) {
      turnosListPanel.innerHTML =
        '<p class="turnos-list-empty">No hay turnos ocupados en este periodo. Use «Nuevo turno» o cambie la fecha.</p>';
      agendaMetrics.innerHTML =
        '<span class="metric-pill">Turnos ocupados: <strong>0</strong></span>';
      return;
    }

    const rows = appointments
      .map((appt) => {
        const patient = patients.find((p) => p.id === appt.patientId);
        const doctor = doctors.find((d) => d.id === appt.doctorId);
        const protocol = cfg.protocols.find((p) => p.id === (patient && patient.protocolId));
        const start = new Date(appt.start);
        const end = storage.appointmentEnd(appt);
        const dateStr = start.toLocaleDateString("es-UY", {
          weekday: "short",
          day: "numeric",
          month: "short"
        });
        const patientName = patient
          ? `${patient.firstName} ${patient.lastName}`
          : "Paciente";
        const color = cfg.doctorColors[
          Math.max(0, doctors.findIndex((d) => d.id === appt.doctorId)) % cfg.doctorColors.length
        ];
        const conflict = conflicts.has(appt.id);
        const selected = state.selectedAppointmentId === appt.id;
        return (
          `<tr class="turno-row${selected ? " is-selected" : ""}${conflict ? " is-conflict" : ""}" data-id="${appt.id}" style="--doctor-color:${color}">` +
          `<td><p class="turno-row__patient">${viewApi.escapeHtml(patientName)}</p>` +
          `<p class="turno-row__meta">CI ${viewApi.escapeHtml(patient ? patient.ci : "—")}</p></td>` +
          `<td>${viewApi.escapeHtml(dateStr)}<br><strong>${formatTime(start)} – ${formatTime(end)}</strong></td>` +
          `<td>${viewApi.escapeHtml(protocol ? protocol.label : "—")}<br><span class="turno-row__meta">${appt.durationMinutes} min</span></td>` +
          `<td>${viewApi.escapeHtml(doctor ? doctor.fullName : "—")}</td>` +
          `<td>Cama ${appt.bedChair}${conflict ? '<br><span class="turno-badge">Conflicto</span>' : ""}</td>` +
          `</tr>`
        );
      })
      .join("");

    turnosListPanel.innerHTML =
      '<table class="turnos-table" aria-label="Turnos ocupados por paciente">' +
      "<thead><tr><th>Paciente</th><th>Fecha y hora</th><th>Protocolo</th><th>Medico</th><th>Cama</th></tr></thead>" +
      "<tbody>" +
      rows +
      "</tbody></table>";

    turnosListPanel.querySelectorAll(".turno-row").forEach((row) => {
      row.addEventListener("click", () => {
        state.selectedAppointmentId = Number(row.dataset.id);
        detailPanel.classList.add("is-open");
        refresh();
      });
    });

    agendaMetrics.innerHTML =
      `<span class="metric-pill">Turnos ocupados: <strong>${appointments.length}</strong></span>` +
      (conflicts.size
        ? `<span class="metric-pill">Conflictos: <strong style="color:#be123c">${conflicts.size}</strong></span>`
        : "");
  }

  function updateToolbar() {
    agendaTitle.textContent =
      state.screen === "list"
        ? "Turnos ocupados por paciente"
        : state.view === "week"
          ? "Agenda semanal"
          : "Agenda diaria";
    agendaRange.textContent =
      state.screen === "list"
        ? viewApi.formatRange("week", state.baseDate)
        : viewApi.getViewTitle() || viewApi.formatRange(state.view, state.baseDate);
    $("btn-new-appointment").disabled = !canWrite();
    $("btn-edit-appointment").disabled = !state.selectedAppointmentId || !selectedAppointmentEditable();
    $("btn-delete-appointment").disabled = !state.selectedAppointmentId || !canDelete();
  }

  function selectedAppointmentEditable() {
    const appt = findAppointment(state.selectedAppointmentId);
    return canEditAppointment(appt);
  }

  function findAppointment(id) {
    return storage.load().appointments.find((a) => a.id === id) || null;
  }

  function renderDoctors() {
    const doctors = storage.getDoctors();
    const appointments = loadAppointments();
    const query = ($("doctor-search").value || "").trim().toLowerCase();

    doctorList.innerHTML = "";

    const allBtn = document.createElement("button");
    allBtn.type = "button";
    allBtn.className = "doctor-card" + (state.selectedDoctorId ? "" : " is-active");
    allBtn.innerHTML =
      '<div class="doctor-card__avatar" style="background:#5f7388">*</div>' +
      '<div><p class="doctor-card__name">Todos</p><p class="doctor-card__meta">Ver agenda completa</p></div>' +
      `<span class="doctor-card__count">${appointments.length}</span>`;
    allBtn.addEventListener("click", () => {
      state.selectedDoctorId = null;
      refresh();
    });
    doctorList.appendChild(allBtn);

    doctors
      .filter((d) => !query || d.fullName.toLowerCase().includes(query) || d.username.toLowerCase().includes(query))
      .forEach((doctor, index) => {
        const count = viewApi.countByDoctor(appointments, doctor.id);
        const color = cfg.doctorColors[index % cfg.doctorColors.length];
        const initials = doctor.fullName
          .split(" ")
          .map((w) => w[0])
          .join("")
          .slice(0, 2)
          .toUpperCase();
        const card = document.createElement("button");
        card.type = "button";
        card.className = "doctor-card" + (state.selectedDoctorId === doctor.id ? " is-active" : "");
        card.innerHTML =
          `<div class="doctor-card__avatar" style="background:${color}">${initials}</div>` +
          `<div><p class="doctor-card__name">${viewApi.escapeHtml(doctor.fullName)}</p>` +
          `<p class="doctor-card__meta">@${viewApi.escapeHtml(doctor.username)}</p></div>` +
          `<span class="doctor-card__count">${count}</span>`;
        card.addEventListener("click", () => {
          state.selectedDoctorId = doctor.id;
          refresh();
        });
        doctorList.appendChild(card);
      });
  }

  function renderAgenda() {
    const appointments = loadAppointments();
    const patients = storage.getPatients();
    const doctors = storage.getDoctors();

    const result = viewApi.render({
      root: agendaRoot,
      view: state.view,
      baseDate: state.baseDate,
      appointments,
      patients,
      doctors,
      doctorFilterId: state.selectedDoctorId,
      selectedId: state.selectedAppointmentId,
      canEdit: canWrite(),
      onSlotClick: openNewAppointment,
      onSelect: (id) => {
        state.selectedAppointmentId = id;
        detailPanel.classList.add("is-open");
        refresh();
      },
      onDatesChange: () => {
        syncBaseDateFromCalendar();
      }
    });

    const m = result.metrics;
    agendaMetrics.innerHTML =
      `<span class="metric-pill">Turnos: <strong>${m.appointmentCount}</strong></span>` +
      `<span class="metric-pill">Bloques ocupados: <strong>${m.occupied}</strong></span>` +
      (m.conflictCount
        ? `<span class="metric-pill">Conflictos: <strong style="color:#be123c">${m.conflictCount}</strong></span>`
        : "");
  }

  function renderDetail() {
    const appt = findAppointment(state.selectedAppointmentId);
    if (!appt) {
      detailContent.innerHTML = '<p class="detail-empty">Seleccione un turno en la agenda para ver el detalle.</p>';
      return;
    }

    const patient = storage.getPatient(appt.patientId);
    const doctor = storage.getUser(appt.doctorId);
    const protocol = cfg.protocols.find((p) => p.id === (patient && patient.protocolId));
    const start = new Date(appt.start);
    const end = storage.appointmentEnd(appt);
    const alerts = [];
    if (patient && patient.neutropenic) {
      alerts.push("Neutropenia");
    }
    if (patient && patient.fever) {
      alerts.push("Fiebre");
    }

    detailContent.innerHTML =
      '<div class="detail-card">' +
      row("Paciente", patient ? `${patient.firstName} ${patient.lastName}` : "—") +
      row("CI", patient ? patient.ci : "—") +
      row("Prestador", patient ? patient.provider || "—" : "—") +
      row("Protocolo", protocol ? protocol.label : "—") +
      (protocol
        ? row(
            "Ocupacion estimada",
            "Infusion " + protocol.infusionMinutes + " min · Butaca " + protocol.duration + " min"
          )
        : "") +
      (patient && patient.scalpCooling ? row("Casco enfriamiento", "Si — scalp cooling") : "") +
      row("Medico", doctor ? doctor.fullName : "—") +
      row("Horario", formatTime(start) + " – " + formatTime(end) + " (" + appt.durationMinutes + " min)") +
      row("Cama / Butaca", String(appt.bedChair)) +
      row("Diagnostico", patient ? patient.diagnosis || "—" : "—") +
      (alerts.length ? row("Alertas", alerts.join(", ")) : "") +
      "</div>";
  }

  function row(label, value) {
    return (
      '<div class="detail-row"><span>' +
      viewApi.escapeHtml(label) +
      '</span><strong>' +
      viewApi.escapeHtml(value) +
      "</strong></div>"
    );
  }

  function formatTime(date) {
    return String(date.getHours()).padStart(2, "0") + ":" + String(date.getMinutes()).padStart(2, "0");
  }

  function openNewAppointment(slot) {
    state.editingId = null;
    $("appointment-dialog-title").textContent = "Nuevo turno";
    fillAppointmentForm({
      patientId: "",
      doctorId: state.selectedDoctorId || storage.getDoctors()[0]?.id || "",
      date: slot.dateIso,
      time: slot.time,
      duration: 60,
      bed: slot.bed
    });
    apptWarning.hidden = true;
    appointmentDialog.showModal();
  }

  function openEditAppointment() {
    const appt = findAppointment(state.selectedAppointmentId);
    if (!appt || !canEditAppointment(appt)) {
      return;
    }
    state.editingId = appt.id;
    $("appointment-dialog-title").textContent = "Editar turno";
    fillAppointmentForm({
      patientId: appt.patientId,
      doctorId: appt.doctorId,
      date: appt.start.slice(0, 10),
      time: appt.start.slice(11, 16),
      duration: appt.durationMinutes,
      bed: appt.bedChair
    });
    apptWarning.hidden = true;
    appointmentDialog.showModal();
  }

  function fillAppointmentForm(values) {
    const patientSelect = $("appt-patient");
    const doctorSelect = $("appt-doctor");
    const patients = storage.getPatients();
    const doctors = storage.getDoctors();

    patientSelect.innerHTML = patients
      .map(
        (p) =>
          `<option value="${p.id}"${p.id === values.patientId ? " selected" : ""}>${viewApi.escapeHtml(p.firstName + " " + p.lastName)} (${viewApi.escapeHtml(p.ci)})</option>`
      )
      .join("");

    doctorSelect.innerHTML = doctors
      .map(
        (d) =>
          `<option value="${d.id}"${d.id === values.doctorId ? " selected" : ""}>${viewApi.escapeHtml(d.fullName)}</option>`
      )
      .join("");

    $("appt-date").value = values.date;
    $("appt-time").value = values.time;
    $("appt-duration").value = values.duration;
    $("appt-bed").value = values.bed;
  }

  function saveAppointmentFromForm(event) {
    event.preventDefault();
    apptWarning.hidden = true;

    const appt = {
      id: state.editingId || 0,
      patientId: Number($("appt-patient").value),
      doctorId: Number($("appt-doctor").value),
      start: $("appt-date").value + "T" + $("appt-time").value + ":00",
      durationMinutes: Number($("appt-duration").value),
      bedChair: Number($("appt-bed").value)
    };

    if (state.editingId) {
      const existing = findAppointment(state.editingId);
      appt.createdById = existing.createdById;
    }

    try {
      const saved = storage.saveAppointment(appt, state.user, !!state.editingId);
      appointmentDialog.close();
      state.selectedAppointmentId = saved.id;
      refresh();
    } catch (err) {
      apptWarning.textContent = err.message;
      apptWarning.hidden = false;
    }
  }

  function deleteSelected() {
    if (!state.selectedAppointmentId || !canDelete()) {
      return;
    }
    if (!confirm("¿Eliminar este turno?")) {
      return;
    }
    try {
      storage.deleteAppointment(state.selectedAppointmentId, state.user);
      state.selectedAppointmentId = null;
      refresh();
    } catch (err) {
      alert(err.message);
    }
  }

  loginForm.addEventListener("submit", (e) => {
    e.preventDefault();
    loginError.hidden = true;
    const user = storage.authenticate($("login-username").value, $("login-password").value);
    if (!user) {
      loginError.textContent = "Usuario o contrasena incorrectos.";
      loginError.hidden = false;
      return;
    }
    state.user = user;
    state.baseDate = startOfWeek(new Date());
    state.screen = "list";
    showApp();
  });

  $("btn-logout").addEventListener("click", () => {
    viewApi.destroy();
    showLogin();
  });

  function shiftPeriod(deltaDays) {
    state.baseDate = storage.addDays(state.baseDate, deltaDays);
    baseDateInput.value = toIsoDate(state.baseDate);
    if (state.screen === "calendar") {
      viewApi.gotoDate(state.baseDate);
    }
  }

  $("btn-prev").addEventListener("click", () => {
    if (state.screen === "calendar") {
      viewApi.navigatePrev();
      syncBaseDateFromCalendar();
    } else {
      shiftPeriod(-7);
    }
    refresh();
  });

  $("btn-next").addEventListener("click", () => {
    if (state.screen === "calendar") {
      viewApi.navigateNext();
      syncBaseDateFromCalendar();
    } else {
      shiftPeriod(7);
    }
    refresh();
  });

  $("btn-today").addEventListener("click", () => {
    state.baseDate = startOfWeek(new Date());
    baseDateInput.value = toIsoDate(state.baseDate);
    if (state.screen === "calendar") {
      viewApi.navigateToday();
      syncBaseDateFromCalendar();
    }
    refresh();
  });

  baseDateInput.addEventListener("change", () => {
    const picked = parseInputDate(baseDateInput.value);
    state.baseDate = startOfWeek(picked);
    baseDateInput.value = toIsoDate(state.baseDate);
    if (state.screen === "calendar") {
      viewApi.gotoDate(state.baseDate);
    }
    refresh();
  });

  document.querySelectorAll("[data-screen]").forEach((btn) => {
    btn.addEventListener("click", () => {
      state.screen = btn.dataset.screen;
      if (state.screen === "calendar") {
        state.view = btn.dataset.view || "week";
        viewApi.gotoDate(state.baseDate);
      }
      syncScreenTabs();
      refresh();
    });
  });

  $("btn-new-appointment").addEventListener("click", () => {
    openNewAppointment({
      dateIso: toIsoDate(state.baseDate),
      time: cfg.workStart,
      bed: 1
    });
  });

  $("btn-edit-appointment").addEventListener("click", openEditAppointment);
  $("btn-delete-appointment").addEventListener("click", deleteSelected);
  appointmentForm.addEventListener("submit", saveAppointmentFromForm);

  document.querySelectorAll("[data-close]").forEach((btn) => {
    btn.addEventListener("click", () => appointmentDialog.close());
  });

  $("doctor-search").addEventListener("input", renderDoctors);

  $("btn-toggle-sidebar").addEventListener("click", () => {
    const sidebar = document.querySelector(".doctor-sidebar");
    sidebar.classList.toggle("is-collapsed");
    $("btn-toggle-sidebar").textContent = sidebar.classList.contains("is-collapsed")
      ? "Mostrar"
      : "Ocultar";
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && appointmentDialog.open) {
      appointmentDialog.close();
    }
  });
})();
