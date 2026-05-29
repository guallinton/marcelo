package com.oncologia.agenda.dao;

import com.oncologia.agenda.model.Appointment;
import com.oncologia.agenda.model.ChemoProtocol;
import com.oncologia.agenda.model.Patient;
import com.oncologia.agenda.model.Role;
import com.oncologia.agenda.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppointmentDao {
    public List<Appointment> findBetween(LocalDate from, LocalDate to) {
        String sql = appointmentSelect()
                + " WHERE a.start_time >= ? AND a.start_time < ?"
                + " ORDER BY a.start_time, a.bed_chair";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = statement.executeQuery()) {
                List<Appointment> appointments = new ArrayList<>();
                while (rs.next()) {
                    appointments.add(map(rs));
                }
                return appointments;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar agenda", e);
        }
    }

    public List<Appointment> findByDate(LocalDate date) {
        return findBetween(date, date);
    }

    public Optional<Appointment> findById(long id) {
        return findBetween(LocalDate.now().minusYears(2), LocalDate.now().plusYears(2)).stream()
                .filter(appointment -> appointment.getId() == id)
                .findFirst();
    }

    public Appointment save(Appointment appointment) {
        if (appointment.getId() == 0) {
            return insert(appointment);
        }
        update(appointment);
        return appointment;
    }

    public void delete(long id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM appointments WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el turno", e);
        }
    }

    public boolean patientHasAppointmentOnDate(long patientId, LocalDate date, long ignoreAppointmentId) {
        String sql = "SELECT COUNT(*) FROM appointments "
                + "WHERE patient_id = ? AND CAST(start_time AS DATE) = ? AND id <> ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, patientId);
            statement.setDate(2, Date.valueOf(date));
            statement.setLong(3, ignoreAppointmentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al validar turnos del paciente", e);
        }
    }

    public List<Appointment> findOverlaps(int bedChair, LocalDateTime start, LocalDateTime endWithCleaning, long ignoreAppointmentId) {
        String sql = appointmentSelect()
                + " WHERE a.bed_chair = ?"
                + " AND a.id <> ?"
                + " AND a.start_time < ?"
                + " AND DATEADD('MINUTE', a.duration_minutes + 15, a.start_time) > ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bedChair);
            statement.setLong(2, ignoreAppointmentId);
            statement.setTimestamp(3, Timestamp.valueOf(endWithCleaning));
            statement.setTimestamp(4, Timestamp.valueOf(start));
            try (ResultSet rs = statement.executeQuery()) {
                List<Appointment> overlaps = new ArrayList<>();
                while (rs.next()) {
                    overlaps.add(map(rs));
                }
                return overlaps;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al validar superposicion", e);
        }
    }

    public List<Patient> findPatientsWithoutAppointment(LocalDate from, LocalDate to) {
        String sql = "SELECT * FROM patients p "
                + "WHERE NOT EXISTS ("
                + "SELECT 1 FROM appointments a "
                + "WHERE a.patient_id = p.id AND CAST(a.start_time AS DATE) BETWEEN ? AND ?"
                + ") ORDER BY p.last_name, p.first_name";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet rs = statement.executeQuery()) {
                List<Patient> patients = new ArrayList<>();
                while (rs.next()) {
                    patients.add(mapStandalonePatient(rs));
                }
                return patients;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al consultar pacientes pendientes", e);
        }
    }

    private Appointment insert(Appointment appointment) {
        String sql = "INSERT INTO appointments (patient_id, doctor_id, created_by, start_time, duration_minutes, bed_chair) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(statement, appointment);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    appointment.setId(keys.getLong(1));
                }
            }
            return appointment;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar turno", e);
        }
    }

    private void update(Appointment appointment) {
        String sql = "UPDATE appointments "
                + "SET patient_id = ?, doctor_id = ?, created_by = ?, start_time = ?, duration_minutes = ?, bed_chair = ? "
                + "WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fill(statement, appointment);
            statement.setLong(7, appointment.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar turno", e);
        }
    }

    private void fill(PreparedStatement statement, Appointment appointment) throws SQLException {
        statement.setLong(1, appointment.getPatient().getId());
        statement.setLong(2, appointment.getDoctor().getId());
        statement.setLong(3, appointment.getCreatedBy().getId());
        statement.setTimestamp(4, Timestamp.valueOf(appointment.getStart()));
        statement.setInt(5, appointment.getDurationMinutes());
        statement.setInt(6, appointment.getBedChair());
    }

    private String appointmentSelect() {
        return "SELECT a.*, p.first_name, p.last_name, p.dni, p.insurance, p.diagnosis, p.protocol,"
                + " p.allergies, p.emergency_contact, p.neutropenic, p.fever,"
                + " d.username doctor_username, d.password doctor_password, d.full_name doctor_name, d.role doctor_role,"
                + " c.username creator_username, c.password creator_password, c.full_name creator_name, c.role creator_role"
                + " FROM appointments a"
                + " JOIN patients p ON p.id = a.patient_id"
                + " JOIN users d ON d.id = a.doctor_id"
                + " JOIN users c ON c.id = a.created_by";
    }

    private Appointment map(ResultSet rs) throws SQLException {
        Appointment appointment = new Appointment();
        appointment.setId(rs.getLong("id"));
        appointment.setPatient(mapAppointmentPatient(rs));
        appointment.setDoctor(new User(
                rs.getLong("doctor_id"),
                rs.getString("doctor_username"),
                rs.getString("doctor_password"),
                rs.getString("doctor_name"),
                Role.valueOf(rs.getString("doctor_role"))
        ));
        appointment.setCreatedBy(new User(
                rs.getLong("created_by"),
                rs.getString("creator_username"),
                rs.getString("creator_password"),
                rs.getString("creator_name"),
                Role.valueOf(rs.getString("creator_role"))
        ));
        appointment.setStart(rs.getTimestamp("start_time").toLocalDateTime());
        appointment.setDurationMinutes(rs.getInt("duration_minutes"));
        appointment.setBedChair(rs.getInt("bed_chair"));
        return appointment;
    }

    private Patient mapAppointmentPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getLong("patient_id"));
        fillPatient(rs, patient);
        return patient;
    }

    private Patient mapStandalonePatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getLong("id"));
        fillPatient(rs, patient);
        return patient;
    }

    private void fillPatient(ResultSet rs, Patient patient) throws SQLException {
        patient.setFirstName(rs.getString("first_name"));
        patient.setLastName(rs.getString("last_name"));
        patient.setDni(rs.getString("dni"));
        patient.setInsurance(rs.getString("insurance"));
        patient.setDiagnosis(rs.getString("diagnosis"));
        patient.setProtocol(ChemoProtocol.valueOf(rs.getString("protocol")));
        patient.setAllergies(rs.getString("allergies"));
        patient.setEmergencyContact(rs.getString("emergency_contact"));
        patient.setNeutropenic(rs.getBoolean("neutropenic"));
        patient.setFever(rs.getBoolean("fever"));
    }
}
