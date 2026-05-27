package com.oncologia.agenda.service;

import com.oncologia.agenda.dao.AppointmentDao;
import com.oncologia.agenda.dao.ConfigDao;
import com.oncologia.agenda.dao.UserDao;
import com.oncologia.agenda.model.Appointment;
import com.oncologia.agenda.model.Patient;
import com.oncologia.agenda.model.Role;
import com.oncologia.agenda.model.ScheduleConfig;
import com.oncologia.agenda.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScheduleService {
    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final ConfigDao configDao = new ConfigDao();
    private final UserDao userDao = new UserDao();

    public ScheduleConfig getConfig() {
        return configDao.load();
    }

    public void saveConfig(ScheduleConfig config) {
        if (!config.getWorkEnd().isAfter(config.getWorkStart())) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (config.getSlotMinutes() != 30 && config.getSlotMinutes() != 60) {
            throw new ValidationException("La duracion del bloque debe ser 30 o 60 minutos.");
        }
        if (config.getBedCount() < 1) {
            throw new ValidationException("Debe existir al menos una cama/butaca.");
        }
        configDao.save(config);
    }

    public List<User> findDoctors() {
        return userDao.findDoctors();
    }

    public List<Appointment> appointmentsBetween(LocalDate from, LocalDate to) {
        return appointmentDao.findBetween(from, to);
    }

    public List<Appointment> appointmentsOn(LocalDate date) {
        return appointmentDao.findByDate(date);
    }

    public Appointment saveAppointment(Appointment appointment, User currentUser) {
        validateCanWrite(appointment, currentUser, false);
        validateAppointment(appointment);
        return appointmentDao.save(appointment);
    }

    public Appointment reschedule(Appointment appointment, User currentUser) {
        validateCanWrite(appointment, currentUser, true);
        validateAppointment(appointment);
        return appointmentDao.save(appointment);
    }

    public void deleteAppointment(Appointment appointment, User currentUser) {
        if (currentUser.getRole() != Role.ENFERMERIA) {
            throw new ValidationException("Solo enfermeria puede eliminar turnos.");
        }
        appointmentDao.delete(appointment.getId());
    }

    public List<Patient> pendingPatientsNextSevenDays() {
        LocalDate today = LocalDate.now();
        return appointmentDao.findPatientsWithoutAppointment(today, today.plusDays(7));
    }

    public Map<LocalDate, Long> dailyOccupancy(LocalDate from, LocalDate to) {
        return appointmentDao.findBetween(from, to).stream()
                .collect(Collectors.groupingBy(a -> a.getStart().toLocalDate(), Collectors.counting()));
    }

    private void validateCanWrite(Appointment appointment, User currentUser, boolean reschedule) {
        if (currentUser.getRole() == Role.ENFERMERIA) {
            return;
        }
        if (appointment.getId() == 0) {
            return;
        }
        if (reschedule && appointment.getCreatedBy().getId() == currentUser.getId()) {
            return;
        }
        throw new ValidationException("El medico solo puede crear turnos o reprogramar los propios.");
    }

    private void validateAppointment(Appointment appointment) {
        if (appointment.getPatient() == null || appointment.getDoctor() == null) {
            throw new ValidationException("Debe seleccionar paciente y medico.");
        }
        if (appointment.getStart() == null) {
            throw new ValidationException("Debe seleccionar fecha y hora.");
        }
        if (appointment.getDurationMinutes() <= 0) {
            throw new ValidationException("La duracion debe ser mayor a cero.");
        }
        ScheduleConfig config = getConfig();
        if (appointment.getBedChair() < 1 || appointment.getBedChair() > config.getBedCount()) {
            throw new ValidationException("La cama/butaca seleccionada no existe.");
        }
        LocalDateTime start = appointment.getStart();
        if (start.toLocalTime().isBefore(config.getWorkStart()) || start.toLocalTime().isAfter(config.getWorkEnd().minusMinutes(1))) {
            throw new ValidationException("El turno debe iniciar dentro del horario laboral configurado.");
        }
        if (appointment.getEndWithCleaning().toLocalTime().isAfter(config.getWorkEnd())) {
            throw new ValidationException("El turno mas limpieza excede el horario laboral.");
        }
        if (appointmentDao.patientHasAppointmentOnDate(appointment.getPatient().getId(), start.toLocalDate(), appointment.getId())) {
            throw new ValidationException("El paciente ya tiene un turno ese dia.");
        }
        boolean overlaps = !appointmentDao.findOverlaps(
                appointment.getBedChair(),
                appointment.getStart(),
                appointment.getEndWithCleaning(),
                appointment.getId()
        ).isEmpty();
        if (overlaps) {
            throw new ValidationException("Existe superposicion en la misma cama/butaca considerando 15 minutos de limpieza.");
        }
    }
}
