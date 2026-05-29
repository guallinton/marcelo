window.AgendaStorage = (function () {
  const cfg = window.AgendaConfig;

  function defaultData() {
    const today = new Date();
    const y = today.getFullYear();
    const m = String(today.getMonth() + 1).padStart(2, "0");
    const d = String(today.getDate()).padStart(2, "0");
    const base = `${y}-${m}-${d}`;

    return {
      version: 1,
      users: [
        { id: 1, username: "enfermera", password: "1234", fullName: "Lic. Enfermeria", role: "ENFERMERIA" },
        { id: 2, username: "drlopez", password: "1234", fullName: "Dr. Lopez", role: "MEDICO" }
      ],
      patients: [
        {
          id: 1,
          firstName: "Maria",
          lastName: "Gonzalez",
          ci: "4.455.354-2",
          provider: "ASSE",
          doctorId: 2,
          diagnosis: "Ca colon",
          protocolId: "FOLFOX",
          allergies: "Ninguna",
          neutropenic: false,
          fever: false,
          scalpCooling: false
        },
        {
          id: 2,
          firstName: "Juan",
          lastName: "Pereira",
          ci: "3.456.789-0",
          provider: "CASMU",
          doctorId: 2,
          diagnosis: "Linfoma",
          protocolId: "RITUXIMAB",
          allergies: "Penicilina",
          neutropenic: true,
          fever: false,
          scalpCooling: true
        },
        {
          id: 3,
          firstName: "Ana",
          lastName: "Rodriguez",
          ci: "5.123.456-7",
          provider: "Blue Cross",
          doctorId: 2,
          diagnosis: "Ca mama",
          protocolId: "AC_T",
          allergies: "",
          neutropenic: false,
          fever: false,
          scalpCooling: false
        },
        {
          id: 4,
          firstName: "Carlos",
          lastName: "Silva",
          ci: "2.987.654-3",
          provider: "MP",
          doctorId: 2,
          diagnosis: "Ca pulmon",
          protocolId: "CARBO_PACL",
          allergies: "",
          neutropenic: false,
          fever: true
        }
      ],
      appointments: seedAppointments(base),
      nextId: { user: 3, patient: 5, appointment: 20 }
    };
  }

  function seedAppointments(isoDate) {
    const day = parseIsoDate(isoDate);
    const monday = startOfWeek(day);
    const fmt = (date, time) => `${toIsoDate(date)}T${time}:00`;

    return [
      {
        id: 1,
        patientId: 1,
        doctorId: 2,
        createdById: 1,
        start: fmt(addDays(monday, 0), "08:30"),
        durationMinutes: 180,
        bedChair: 1
      },
      {
        id: 2,
        patientId: 2,
        doctorId: 2,
        createdById: 2,
        start: fmt(addDays(monday, 1), "09:00"),
        durationMinutes: 120,
        bedChair: 2
      },
      {
        id: 3,
        patientId: 3,
        doctorId: 2,
        createdById: 1,
        start: fmt(addDays(monday, 2), "10:00"),
        durationMinutes: 60,
        bedChair: 1
      },
      {
        id: 4,
        patientId: 4,
        doctorId: 2,
        createdById: 2,
        start: fmt(addDays(monday, 3), "08:00"),
        durationMinutes: 240,
        bedChair: 3
      },
      {
        id: 5,
        patientId: 1,
        doctorId: 2,
        createdById: 1,
        start: fmt(addDays(monday, 4), "14:00"),
        durationMinutes: 90,
        bedChair: 2
      }
    ];
  }

  function load() {
    try {
      const raw = localStorage.getItem(cfg.storageKey);
      if (!raw) {
        const data = defaultData();
        save(data);
        return data;
      }
      const data = JSON.parse(raw);
      alignDemoAppointmentsToCurrentWeek(data);
      save(data);
      return data;
    } catch (e) {
      const data = defaultData();
      save(data);
      return data;
    }
  }

  /** Reubica turnos demo (id 1-5) a la semana actual si no hay turnos visibles. */
  function alignDemoAppointmentsToCurrentWeek(data) {
    const monday = startOfWeek(new Date());
    const from = new Date(toIsoDate(monday) + "T00:00:00");
    const to = new Date(toIsoDate(addDays(monday, 6)) + "T23:59:59");
    const inWeek = data.appointments.filter((a) => {
      const s = new Date(a.start);
      return s >= from && s <= to;
    });
    if (inWeek.length > 0) {
      return;
    }
    const fmt = (date, time) => `${toIsoDate(date)}T${time}:00`;
    const plan = [
      { id: 1, day: 0, time: "08:30", durationMinutes: 180, bedChair: 1, patientId: 1 },
      { id: 2, day: 1, time: "09:00", durationMinutes: 120, bedChair: 2, patientId: 2 },
      { id: 3, day: 2, time: "10:00", durationMinutes: 60, bedChair: 1, patientId: 3 },
      { id: 4, day: 3, time: "08:00", durationMinutes: 240, bedChair: 3, patientId: 4 },
      { id: 5, day: 4, time: "14:00", durationMinutes: 90, bedChair: 2, patientId: 1 }
    ];
    plan.forEach((item) => {
      let appt = data.appointments.find((a) => a.id === item.id);
      if (!appt) {
        appt = {
          id: item.id,
          patientId: item.patientId,
          doctorId: 2,
          createdById: 1,
          start: fmt(addDays(monday, item.day), item.time),
          durationMinutes: item.durationMinutes,
          bedChair: item.bedChair
        };
        data.appointments.push(appt);
        return;
      }
      appt.start = fmt(addDays(monday, item.day), item.time);
      appt.durationMinutes = item.durationMinutes;
      appt.bedChair = item.bedChair;
    });
  }

  function save(data) {
    localStorage.setItem(cfg.storageKey, JSON.stringify(data));
  }

  function getData() {
    return load();
  }

  function persist(mutator) {
    const data = load();
    mutator(data);
    save(data);
    return data;
  }

  function authenticate(username, password) {
    const user = load().users.find(
      (u) => u.username === username.trim() && u.password === password
    );
    return user ? { ...user } : null;
  }

  function getDoctors() {
    return load().users.filter((u) => u.role === "MEDICO");
  }

  function getPatients() {
    return load().patients.slice();
  }

  function getPatient(id) {
    return load().patients.find((p) => p.id === id) || null;
  }

  function getUser(id) {
    return load().users.find((u) => u.id === id) || null;
  }

  function getAppointmentsBetween(fromIso, toIso) {
    const from = new Date(fromIso + "T00:00:00");
    const to = new Date(toIso + "T23:59:59");
    return load().appointments.filter((a) => {
      const start = new Date(a.start);
      return start >= from && start <= to;
    });
  }

  function appointmentEndWithCleaning(appt) {
    return addMinutes(new Date(appt.start), appt.durationMinutes + cfg.cleaningMinutes);
  }

  function appointmentEnd(appt) {
    return addMinutes(new Date(appt.start), appt.durationMinutes);
  }

  function findOverlaps(appt, excludeId) {
    const endClean = appointmentEndWithCleaning(appt);
    const start = new Date(appt.start);
    return load().appointments.filter((other) => {
      if (other.id === excludeId || other.id === appt.id) {
        return false;
      }
      if (other.bedChair !== appt.bedChair) {
        return false;
      }
      const oStart = new Date(other.start);
      const oEnd = appointmentEndWithCleaning(other);
      return start < oEnd && endClean > oStart;
    });
  }

  function patientHasAppointmentOnDate(patientId, dateIso, excludeId) {
    return load().appointments.some((a) => {
      if (a.id === excludeId) {
        return false;
      }
      if (a.patientId !== patientId) {
        return false;
      }
      return a.start.slice(0, 10) === dateIso;
    });
  }

  function validateAppointment(appt, excludeId) {
    const patient = getPatient(appt.patientId);
    const doctor = getUser(appt.doctorId);
    if (!patient || !doctor) {
      return "Debe seleccionar paciente y medico.";
    }
    if (!appt.start) {
      return "Debe seleccionar fecha y hora.";
    }
    if (!appt.durationMinutes || appt.durationMinutes <= 0) {
      return "La duracion debe ser mayor a cero.";
    }
    if (appt.bedChair < 1 || appt.bedChair > cfg.bedCount) {
      return "La cama/butaca seleccionada no existe.";
    }

    const start = new Date(appt.start);
    const workStart = parseTime(cfg.workStart);
    const workEnd = parseTime(cfg.workEnd);
    const startMin = start.getHours() * 60 + start.getMinutes();
    const endClean = appointmentEndWithCleaning(appt);
    const endCleanMin = endClean.getHours() * 60 + endClean.getMinutes();

    if (startMin < workStart || startMin >= workEnd) {
      return "El turno debe iniciar dentro del horario laboral (" + cfg.workStart + " - " + cfg.workEnd + ").";
    }
    if (endCleanMin > workEnd) {
      return "El turno mas limpieza excede el horario laboral.";
    }

    const dateIso = appt.start.slice(0, 10);
    if (patientHasAppointmentOnDate(appt.patientId, dateIso, excludeId || appt.id)) {
      return "El paciente ya tiene un turno ese dia.";
    }

    const overlaps = findOverlaps(appt, excludeId || appt.id);
    if (overlaps.length) {
      return "Existe superposicion en la misma cama/butaca (incluye 15 min de limpieza).";
    }

    return null;
  }

  function saveAppointment(appt, currentUser, isEdit) {
    const error = validateAppointment(appt, isEdit ? appt.id : 0);
    if (error) {
      throw new Error(error);
    }

    if (currentUser.role === "MEDICO" && isEdit) {
      const existing = load().appointments.find((a) => a.id === appt.id);
      if (existing && existing.createdById !== currentUser.id) {
        throw new Error("El medico solo puede editar turnos creados por el mismo.");
      }
    }

    persist((data) => {
      if (appt.id) {
        const idx = data.appointments.findIndex((a) => a.id === appt.id);
        if (idx >= 0) {
          data.appointments[idx] = { ...appt };
        }
      } else {
        appt.id = data.nextId.appointment++;
        appt.createdById = currentUser.id;
        data.appointments.push({ ...appt });
      }
    });
    return { ...appt };
  }

  function deleteAppointment(id, currentUser) {
    if (currentUser.role !== "ENFERMERIA") {
      throw new Error("Solo enfermeria puede eliminar turnos.");
    }
    persist((data) => {
      data.appointments = data.appointments.filter((a) => a.id !== id);
    });
  }

  function resetDemo() {
    save(defaultData());
  }

  function parseIsoDate(iso) {
    const [y, m, d] = iso.split("-").map(Number);
    return new Date(y, m - 1, d);
  }

  function toIsoDate(date) {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    return `${y}-${m}-${d}`;
  }

  function addDays(date, days) {
    const copy = new Date(date);
    copy.setDate(copy.getDate() + days);
    return copy;
  }

  function startOfWeek(date) {
    const copy = new Date(date);
    const day = copy.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    copy.setDate(copy.getDate() + diff);
    return copy;
  }

  function addMinutes(date, minutes) {
    return new Date(date.getTime() + minutes * 60000);
  }

  function parseTime(hhmm) {
    const [h, m] = hhmm.split(":").map(Number);
    return h * 60 + m;
  }

  return {
    load,
    save,
    getData,
    authenticate,
    getDoctors,
    getPatients,
    getPatient,
    getUser,
    getAppointmentsBetween,
    saveAppointment,
    deleteAppointment,
    validateAppointment,
    findOverlaps,
    appointmentEnd,
    appointmentEndWithCleaning,
    alignDemoAppointmentsToCurrentWeek,
    resetDemo,
    toIsoDate,
    parseIsoDate,
    addDays,
    startOfWeek
  };
})();
