(function () {
  const cfg = window.AgendaConfig;
  const storage = window.AgendaStorage;
  const viewApi = window.AgendaView;

  const state = {
    user: null,
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
    $("session-user").textContent = state.user.fullName;
    const badge = $("session-role");
    badge.textContent = roleLabel(state.user.role);
    badge.className = "badge " + (state.user.role === "ENFERMERIA" ? "badge--nurse" : "badge--doctor");
    baseDateInput.value = toIsoDate(state.baseDate);
    refresh();
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
    renderAgenda();
    renderDetail();
    updateToolbar();
  }

  function updateToolbar() {
    agendaTitle.textContent = state.view === "week" ? "Agenda semanal" : "Agenda diaria";
    agendaRange.textContent =
      viewApi.getViewTitle() || viewApi.formatRange(state.view, state.baseDate);
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
    showApp();
  });

  $("btn-logout").addEventListener("click", () => {
    viewApi.destroy();
    showLogin();
  });

  $("btn-prev").addEventListener("click", () => {
    viewApi.navigatePrev();
    syncBaseDateFromCalendar();
    refresh();
  });

  $("btn-next").addEventListener("click", () => {
    viewApi.navigateNext();
    syncBaseDateFromCalendar();
    refresh();
  });

  $("btn-today").addEventListener("click", () => {
    viewApi.navigateToday();
    syncBaseDateFromCalendar();
    refresh();
  });

  baseDateInput.addEventListener("change", () => {
    const picked = parseInputDate(baseDateInput.value);
    state.baseDate = state.view === "week" ? startOfWeek(picked) : picked;
    baseDateInput.value = toIsoDate(state.baseDate);
    viewApi.gotoDate(state.baseDate);
    refresh();
  });

  document.querySelectorAll("[data-view]").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll("[data-view]").forEach((b) => b.classList.remove("is-active"));
      btn.classList.add("is-active");
      state.view = btn.dataset.view;
      if (state.view === "week") {
        state.baseDate = startOfWeek(state.baseDate);
      }
      baseDateInput.value = toIsoDate(state.baseDate);
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
