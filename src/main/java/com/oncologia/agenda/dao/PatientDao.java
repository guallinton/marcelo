package com.oncologia.agenda.dao;

import com.oncologia.agenda.model.ChemoProtocol;
import com.oncologia.agenda.model.Patient;

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
        String sql = "SELECT * FROM patients "
                + "WHERE LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? OR dni LIKE ? "
                + "ORDER BY last_name, first_name";
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
        String sql = "SELECT * FROM patients WHERE id = ?";
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
                + "(first_name, last_name, dni, insurance, diagnosis, protocol, allergies, emergency_contact, neutropenic, fever) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            throw new DataAccessException("No se pudo crear paciente. DNI duplicado o datos invalidos.", e);
        }
    }

    private void update(Patient patient) {
        String sql = "UPDATE patients "
                + "SET first_name = ?, last_name = ?, dni = ?, insurance = ?, diagnosis = ?, protocol = ?, "
                + "allergies = ?, emergency_contact = ?, neutropenic = ?, fever = ? "
                + "WHERE id = ?";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            fill(statement, patient);
            statement.setLong(11, patient.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar paciente. DNI duplicado o datos invalidos.", e);
        }
    }

    private void fill(PreparedStatement statement, Patient patient) throws SQLException {
        statement.setString(1, patient.getFirstName());
        statement.setString(2, patient.getLastName());
        statement.setString(3, patient.getDni());
        statement.setString(4, patient.getInsurance());
        statement.setString(5, patient.getDiagnosis());
        statement.setString(6, patient.getProtocol().name());
        statement.setString(7, patient.getAllergies());
        statement.setString(8, patient.getEmergencyContact());
        statement.setBoolean(9, patient.isNeutropenic());
        statement.setBoolean(10, patient.isFever());
    }

    private Patient map(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setId(rs.getLong("id"));
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
        return patient;
    }
}
