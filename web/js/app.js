(function () {
  const cfg = window.AgendaConfig;
  const storage = window.AgendaStorage;

  function getViewApi() {
    return window.AgendaView;
  }

  const state = {
    user: null,
    screen: "list",
    view: "week",
    baseDate: null,
    selectedDoctorId: null,
    selectedAppointmentId: null,
    editingId: null
  };

  let $ = function () {};
  let loginScreen, appScreen, loginForm, loginError, agendaRoot, turnosListPanel;
  let doctorList, detailContent, agendaMetrics, agendaTitle, agendaRange, baseDateInput;
  let appointmentDialog, appointmentForm, apptWarning, detailPanel;

  function startOfWeek(date) {
    return storage.startOfWeek(date);
  }

  function toIsoDate(date) {
    return storage.toIsoDate(date);
  }

  function parseInputDate(value) {
    if (!value) return new Date();
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
    if (!state.user || !appt) return false;
    if (state.user.role === "ENFERMERIA") return true;
    return appt.createdById === state.user.id;
  }

  function showLogin() {
    loginScreen.hidden = false;
    appScreen.hidden = true;
    state.user = null;
    state.selectedAppointmentId = null;
  }

  function showApp() {
    loginScreen.hidden = true;
    appScreen.hidden = false;
    state.screen = "list";
    state.selectedAppointmentId = null;
    state.baseDate = startOfWeek(new Date());
    $("session-user").textContent = state.user.fullName;
    const badge = $("session-role");
    badge.textContent = roleLabel(state.user.role);
    badge.className = "badge " + (state.user.role === "ENFERMERIA" ? "badge--nurse" : "badge--doctor");
    baseDateInput.value = toIsoDate(state.baseDate);
    storage.load();
    syncScreenTabs();
    refresh();
  }

  function syncScreenTabs() {
    document.querySelectorAll("[data-screen]").forEach((btn) => {
      btn.classList.toggle("is-active", btn.dataset.screen === state.screen);
    });
    turnosListPanel.hidden = state.screen !== "list";
    agendaRoot.hidden = state.screen !== "calendar";
  }

  function rangeForView() {
    const from = toIsoDate(state.baseDate);
    const to = toIsoDate(storage.addDays(state.baseDate, 6));
    return { from, to };
  }

  function loadAppointments() {
    const { from, to } = rangeForView();
    return storage.getAppointmentsBetween(from, to);
  }

  function syncBaseDateFromCalendar() {
    const viewApi = getViewApi();
    const d = viewApi.getCalendarDate();
    if (!d) return;
    state.baseDate = startOfWeek(d);
    baseDateInput.value = toIsoDate(state.baseDate);
    const title = viewApi.getViewTitle();
    if (title) agendaRange.textContent = title;
  }

  function refresh() {
    if (!state.user) return;
    try {
      renderDoctors();
      if (state.screen === "list") {
        renderTurnosList();
      } else {
        renderAgenda();
      }
      renderDetail();
      updateToolbar();
    } catch (err) {
      console.error("Error al actualizar vista:", err);
      showToast("Error al actualizar la pantalla: " + err.message);
    }
  }

  function showToast(message) {
    let el = document.getElementById("app-toast");
    if (!el) {
      el = document.createElement("div");
      el.id = "app-toast";
      el.className = "app-toast";
      document.body.appendChild(el);
    }
    el.textContent = message;
    el.classList.add("is-visible");
    clearTimeout(showToast._timer);
    showToast._timer = setTimeout(() => el.classList.remove("is-visible"), 4000);
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
    const viewApi = getViewApi();
    const appointments = loadAppointments()
      .filter((a) => !state.selectedDoctorId || a.doctorId === state.selectedDoctorId)
      .sort((a, b) => new Date(a.start) - new Date(b.start));
    const patients = storage.getPatients();
    const doctors = storage.getDoctors();
    const conflicts = computeConflicts(loadAppointments());

    if (!appointments.length) {
      turnosListPanel.innerHTML =
        '<p class="turnos-list-empty">No hay turnos en esta semana. Pulse <strong>Hoy</strong> o <strong>Nuevo turno</strong>.</p>';
      agendaMetrics.innerHTML = '<span class="metric-pill">Turnos ocupados: <strong>0</strong></span>';
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
        const patientName = patient ? `${patient.firstName} ${patient.lastName}` : "Paciente";
        const docIdx = doctors.findIndex((d) => d.id === appt.doctorId);
        const color = cfg.doctorColors[(docIdx >= 0 ? docIdx : 0) % cfg.doctorColors.length];
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
      "<tbody>" + rows + "</tbody></table>";

    agendaMetrics.innerHTML =
      `<span class="metric-pill">Turnos ocupados: <strong>${appointments.length}</strong></span>` +
      (conflicts.size
        ? `<span class="metric-pill">Conflictos: <strong style="color:#be123c">${conflicts.size}</strong></span>`
        : "");
  }

  function updateToolbar() {
    const viewApi = getViewApi();
    agendaTitle.textContent =
      state.screen === "list" ? "Turnos ocupados por paciente" : "Agenda semanal (calendario)";
    agendaRange.textContent =
      state.screen === "list"
        ? viewApi.formatRange("week", state.baseDate)
        : viewApi.getViewTitle() || viewApi.formatRange("week", state.baseDate);
    $("btn-new-appointment").disabled = !canWrite();
    const canEdit = state.selectedAppointmentId && selectedAppointmentEditable();
    $("btn-edit-appointment").disabled = !canEdit;
    $("btn-delete-appointment").disabled = !state.selectedAppointmentId || !canDelete();
  }

  function selectedAppointmentEditable() {
    return canEditAppointment(findAppointment(state.selectedAppointmentId));
  }

  function findAppointment(id) {
    if (!id) return null;
    return storage.load().appointments.find((a) => a.id === id) || null;
  }

  function renderDoctors() {
    const viewApi = getViewApi();
    const doctors = storage.getDoctors();
    const appointments = loadAppointments();
    const query = ($("doctor-search").value || "").trim().toLowerCase();

    doctorList.innerHTML = "";

    const allBtn = document.createElement("button");
    allBtn.type = "button";
    allBtn.className = "doctor-card" + (state.selectedDoctorId ? "" : " is-active");
    allBtn.innerHTML =
      '<div class="doctor-card__avatar" style="background:#5f7388">*</div>' +
      '<div><p class="doctor-card__name">Todos</p><p class="doctor-card__meta">Ver todos los turnos</p></div>' +
      `<span class="doctor-card__count">${appointments.length}</span>`;
    allBtn.addEventListener("click", () => {
      state.selectedDoctorId = null;
      refresh();
    });
    doctorList.appendChild(allBtn);

    doctors
      .filter(
        (d) =>
          !query ||
          d.fullName.toLowerCase().includes(query) ||
          d.username.toLowerCase().includes(query)
      )
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
    const viewApi = getViewApi();
    if (typeof FullCalendar === "undefined") {
      agendaRoot.hidden = false;
      agendaRoot.innerHTML =
        '<p class="turnos-list-empty">Calendario no disponible sin internet (FullCalendar). Use la pestaña <strong>Turnos ocupados</strong>.</p>';
      agendaMetrics.innerHTML = "";
      return;
    }

    const appointments = loadAppointments();
    const result = viewApi.render({
      root: agendaRoot,
      view: state.view,
      baseDate: state.baseDate,
      appointments,
      patients: storage.getPatients(),
      doctors: storage.getDoctors(),
      doctorFilterId: state.selectedDoctorId,
      selectedId: state.selectedAppointmentId,
      canEdit: canWrite(),
      onSlotClick: openNewAppointment,
      onSelect: (id) => {
        state.selectedAppointmentId = id;
        detailPanel.classList.add("is-open");
        refresh();
      },
      onDatesChange: () => syncBaseDateFromCalendar()
    });

    const m = result.metrics;
    agendaMetrics.innerHTML =
      `<span class="metric-pill">Turnos: <strong>${m.appointmentCount}</strong></span>` +
      `<span class="metric-pill">Bloques: <strong>${m.occupied}</strong></span>` +
      (m.conflictCount
        ? `<span class="metric-pill">Conflictos: <strong style="color:#be123c">${m.conflictCount}</strong></span>`
        : "");
  }

  function renderDetail() {
    const viewApi = getViewApi();
    const appt = findAppointment(state.selectedAppointmentId);
    if (!appt) {
      detailContent.innerHTML =
        '<p class="detail-empty">Seleccione un turno de la lista o del calendario.</p>';
      return;
    }

    const patient = storage.getPatient(appt.patientId);
    const doctor = storage.getUser(appt.doctorId);
    const protocol = cfg.protocols.find((p) => p.id === (patient && patient.protocolId));
    const start = new Date(appt.start);
    const end = storage.appointmentEnd(appt);
    const alerts = [];
    if (patient && patient.neutropenic) alerts.push("Neutropenia");
    if (patient && patient.fever) alerts.push("Fiebre");
    if (patient && patient.scalpCooling) alerts.push("Casco enfriamiento");

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
      row("Medico", doctor ? doctor.fullName : "—") +
      row("Horario", formatTime(start) + " – " + formatTime(end) + " (" + appt.durationMinutes + " min)") +
      row("Cama / Butaca", String(appt.bedChair)) +
      row("Diagnostico", patient ? patient.diagnosis || "—" : "—") +
      (alerts.length ? row("Alertas", alerts.join(", ")) : "") +
      "</div>";

    function row(label, value) {
      return (
        '<div class="detail-row"><span>' +
        viewApi.escapeHtml(label) +
        '</span><strong>' +
        viewApi.escapeHtml(value) +
        "</strong></div>"
      );
    }
  }

  function formatTime(date) {
    return String(date.getHours()).padStart(2, "0") + ":" + String(date.getMinutes()).padStart(2, "0");
  }

  function defaultProtocolDuration(patientId) {
    const patient = storage.getPatient(patientId);
    if (!patient) return 60;
    const protocol = cfg.protocols.find((p) => p.id === patient.protocolId);
    return protocol ? protocol.duration : 60;
  }

  function openNewAppointment(slot) {
    if (!canWrite()) return;
    state.editingId = null;
    const patients = storage.getPatients();
    const doctors = storage.getDoctors();
    if (!patients.length) {
      showToast("No hay pacientes cargados.");
      return;
    }
    $("appointment-dialog-title").textContent = "Nuevo turno";
    fillAppointmentForm({
      patientId: patients[0].id,
      doctorId: state.selectedDoctorId || doctors[0]?.id || 2,
      date: slot.dateIso || toIsoDate(state.baseDate),
      time: slot.time || cfg.workStart,
      duration: defaultProtocolDuration(patients[0].id),
      bed: slot.bed || 1
    });
    apptWarning.hidden = true;
    if (typeof appointmentDialog.showModal === "function") {
      appointmentDialog.showModal();
    } else {
      appointmentDialog.setAttribute("open", "");
    }
  }

  function openEditAppointment() {
    const appt = findAppointment(state.selectedAppointmentId);
    if (!appt || !canEditAppointment(appt)) {
      showToast("No puede editar este turno.");
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

  function closeAppointmentDialog() {
    appointmentDialog.close();
    apptWarning.hidden = true;
  }

  function fillAppointmentForm(values) {
    const viewApi = getViewApi();
    const patients = storage.getPatients();
    const doctors = storage.getDoctors();
    const patientId = values.patientId || patients[0]?.id;
    const doctorId = values.doctorId || doctors[0]?.id;

    $("appt-patient").innerHTML = patients
      .map(
        (p) =>
          `<option value="${p.id}"${p.id === patientId ? " selected" : ""}>${viewApi.escapeHtml(
            p.firstName + " " + p.lastName
          )} (${viewApi.escapeHtml(p.ci)})</option>`
      )
      .join("");

    $("appt-doctor").innerHTML = doctors
      .map(
        (d) =>
          `<option value="${d.id}"${d.id === doctorId ? " selected" : ""}>${viewApi.escapeHtml(d.fullName)}</option>`
      )
      .join("");

    $("appt-date").value = values.date;
    $("appt-time").value = values.time;
    $("appt-duration").value = values.duration;
    $("appt-bed").value = values.bed;

    $("appt-patient").onchange = () => {
      const pid = Number($("appt-patient").value);
      if (!state.editingId) {
        $("appt-duration").value = defaultProtocolDuration(pid);
      }
    };
  }

  function saveAppointmentFromForm(event) {
    if (event) event.preventDefault();

    const patients = storage.getPatients();
    if (!patients.length) {
      apptWarning.textContent = "Debe existir al menos un paciente.";
      apptWarning.hidden = false;
      return;
    }

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
      if (existing) appt.createdById = existing.createdById;
    }

    try {
      const saved = storage.saveAppointment(appt, state.user, !!state.editingId);
      closeAppointmentDialog();
      state.selectedAppointmentId = saved.id;
      state.editingId = null;
      refresh();
      showToast("Turno guardado correctamente.");
    } catch (err) {
      apptWarning.textContent = err.message;
      apptWarning.hidden = false;
    }
  }

  function deleteSelected() {
    if (!state.selectedAppointmentId || !canDelete()) {
      showToast("Solo enfermeria puede eliminar turnos.");
      return;
    }
    if (!confirm("¿Eliminar este turno?")) return;
    try {
      storage.deleteAppointment(state.selectedAppointmentId, state.user);
      state.selectedAppointmentId = null;
      refresh();
      showToast("Turno eliminado.");
    } catch (err) {
      alert(err.message);
    }
  }

  function shiftPeriod(deltaDays) {
    state.baseDate = storage.addDays(state.baseDate, deltaDays);
    baseDateInput.value = toIsoDate(state.baseDate);
    if (state.screen === "calendar") {
      getViewApi().gotoDate(state.baseDate);
    }
  }

  function bindEvents() {
    loginForm.addEventListener("submit", (e) => {
      e.preventDefault();
      loginError.hidden = true;
      const user = storage.authenticate(
        $("login-username").value,
        $("login-password").value
      );
      if (!user) {
        loginError.textContent = "Usuario o contrasena incorrectos.";
        loginError.hidden = false;
        return;
      }
      state.user = user;
      showApp();
    });

    $("btn-logout").addEventListener("click", () => {
      getViewApi().destroy();
      agendaRoot.innerHTML = "";
      showLogin();
    });

    $("btn-prev").addEventListener("click", () => {
      if (state.screen === "calendar") {
        getViewApi().navigatePrev();
        syncBaseDateFromCalendar();
      } else {
        shiftPeriod(-7);
      }
      refresh();
    });

    $("btn-next").addEventListener("click", () => {
      if (state.screen === "calendar") {
        getViewApi().navigateNext();
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
        getViewApi().gotoDate(state.baseDate);
        syncBaseDateFromCalendar();
      }
      refresh();
    });

    baseDateInput.addEventListener("change", () => {
      state.baseDate = startOfWeek(parseInputDate(baseDateInput.value));
      baseDateInput.value = toIsoDate(state.baseDate);
      if (state.screen === "calendar") {
        getViewApi().gotoDate(state.baseDate);
      }
      refresh();
    });

    document.querySelectorAll("[data-screen]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.screen = btn.dataset.screen;
        if (state.screen === "calendar") {
          state.view = btn.dataset.view || "week";
        }
        syncScreenTabs();
        refresh();
      });
    });

    $("btn-new-appointment").addEventListener("click", () => openNewAppointment({}));

    $("btn-edit-appointment").addEventListener("click", (e) => {
      e.preventDefault();
      openEditAppointment();
    });

    $("btn-delete-appointment").addEventListener("click", (e) => {
      e.preventDefault();
      deleteSelected();
    });

    appointmentForm.addEventListener("submit", saveAppointmentFromForm);

    appointmentDialog.querySelectorAll("[data-close]").forEach((btn) => {
      btn.addEventListener("click", closeAppointmentDialog);
    });

    $("doctor-search").addEventListener("input", () => {
      renderDoctors();
      if (state.screen === "list") renderTurnosList();
      else renderAgenda();
    });

    $("btn-toggle-sidebar").addEventListener("click", () => {
      const sidebar = document.querySelector(".doctor-sidebar");
      sidebar.classList.toggle("is-collapsed");
      $("btn-toggle-sidebar").textContent = sidebar.classList.contains("is-collapsed")
        ? "Mostrar"
        : "Ocultar";
    });

    turnosListPanel.addEventListener("click", (e) => {
      const row = e.target.closest(".turno-row");
      if (!row) return;
      state.selectedAppointmentId = Number(row.dataset.id);
      detailPanel.classList.add("is-open");
      refresh();
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape" && appointmentDialog.open) {
        closeAppointmentDialog();
      }
    });
  }

  function init() {
    $ = (id) => document.getElementById(id);
    loginScreen = $("login-screen");
    appScreen = $("app-screen");
    loginForm = $("login-form");
    loginError = $("login-error");
    agendaRoot = $("agenda-root");
    turnosListPanel = $("turnos-list-panel");
    doctorList = $("doctor-list");
    detailContent = $("detail-content");
    agendaMetrics = $("agenda-metrics");
    agendaTitle = $("agenda-title");
    agendaRange = $("agenda-range");
    baseDateInput = $("base-date");
    appointmentDialog = $("appointment-dialog");
    appointmentForm = $("appointment-form");
    apptWarning = $("appt-warning");
    detailPanel = document.querySelector(".detail-panel");

    if (!loginForm || !appScreen) {
      console.error("Elementos de la app no encontrados en el HTML.");
      return;
    }

    state.baseDate = startOfWeek(new Date());
    bindEvents();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
