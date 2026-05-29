package com.oncologia.agenda.dao;

import com.oncologia.agenda.model.ChemoProtocol;
import com.oncologia.agenda.model.Patient;
import com.oncologia.agenda.model.Role;
import com.oncologia.agenda.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatientDao {
    public List<Patient> search(String query) {
        String like = "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
        String sql = selectSql()
                + "WHERE LOWER(p.first_name) LIKE ? OR LOWER(p.last_name) LIKE ? OR p.dni LIKE ? "
                + "ORDER BY p.last_name, p.first_name";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setString(3, like);
            try (ResultSet rs = statement.executeQuery()) {
                List<Patient> patients = new ArrayList<>();
                while (rs.next()) {
                    patients.add(map(rs));
                }
                return patients;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al buscar pacientes", e);
        }
    }

    public Optional<Patient> findById(long id) {
        String sql = selectSql() + "WHERE p.id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener paciente", e);
        }
    }

    public Optional<Patient> findByCi(String ci) {
        String sql = selectSql() + "WHERE p.dni = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ci);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error al obtener paciente por CI", e);
        }
    }

    public Patient save(Patient patient) {
        if (patient.getId() == 0) {
            return insert(patient);
        }
        update(patient);
        return patient;
    }

    public void delete(long id) {
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM patients WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el paciente. Verifique que no tenga turnos asignados.", e);
        }
    }

    private Patient insert(Patient patient) {
        String sql = "INSERT INTO patients "
                + "(first_name, last_name, dni, insurance, doctor_id, diagnosis, protocol, allergies, emergency_contact, photo, neutropenic, fever, scalp_cooling) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fill(statement, patient);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    patient.setId(keys.getLong(1));
                }
            }
            return patient;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo crear paciente. CI duplicada o datos invalidos.", e);
        }
    }

    private void update(Patient patient) {
        String sql = "UPDATE patients "
                + "SET first_name = ?, last_name = ?, dni = ?, insurance = ?, doctor_id = ?, diagnosis = ?, protocol = ?, "
                + "allergies = ?, emergency_contact = ?, photo = ?, neutropenic = ?, fever = ?, scalp_cooling = ? "
                + "WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fill(statement, patient);
            statement.setLong(14, patient.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar paciente. CI duplicada o datos invalidos.", e);
        }
    }

    private void fill(PreparedStatement statement, Patient patient) throws SQLException {
        statement.setString(1, patient.getFirstName());
        statement.setString(2, patient.getLastName());
        statement.setString(3, patient.getDni());
        statement.setString(4, patient.getInsurance());
        if (patient.getDoctor() == null || patient.getDoctor().getId() == 0) {
            statement.setNull(5, java.sql.Types.BIGINT);
        } else {
            statement.setLong(5, patient.getDoctor().getId());
        }
        statement.setString(6, patient.getDiagnosis());
        statement.setString(7, patient.getProtocol().name());
        statement.setString(8, patient.getAllergies());
        statement.setString(9, patient.getEmergencyContact());
        statement.setBytes(10, patient.getPhoto());
        statement.setBoolean(11, patient.isNeutropenic());
        statement.setBoolean(12, patient.isFever());
        statement.setBoolean(13, patient.isScalpCooling());
    }

    private Patient map(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getLong("id"));
        patient.setFirstName(rs.getString("first_name"));
        patient.setLastName(rs.getString("last_name"));
        patient.setDni(rs.getString("dni"));
        patient.setInsurance(rs.getString("insurance"));
        long doctorId = rs.getLong("doctor_id");
        if (!rs.wasNull()) {
            patient.setDoctor(new User(
                    doctorId,
                    rs.getString("doctor_username"),
                    "",
                    rs.getString("doctor_name"),
                    Role.MEDICO
            ));
        }
        patient.setDiagnosis(rs.getString("diagnosis"));
        patient.setProtocol(ChemoProtocol.valueOf(rs.getString("protocol")));
        patient.setAllergies(rs.getString("allergies"));
        patient.setEmergencyContact(rs.getString("emergency_contact"));
        patient.setPhoto(rs.getBytes("photo"));
        patient.setNeutropenic(rs.getBoolean("neutropenic"));
        patient.setFever(rs.getBoolean("fever"));
        try {
            patient.setScalpCooling(rs.getBoolean("scalp_cooling"));
        } catch (SQLException ignored) {
            patient.setScalpCooling(false);
        }
        return patient;
    }

    private String selectSql() {
        return "SELECT p.*, d.username doctor_username, d.full_name doctor_name "
                + "FROM patients p "
                + "LEFT JOIN users d ON d.id = p.doctor_id ";
    }
}
