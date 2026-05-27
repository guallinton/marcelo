package com.oncologia.agenda.dao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String DB_URL = "jdbc:h2:./data/agenda_oncologia;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id IDENTITY PRIMARY KEY,
                            username VARCHAR(80) NOT NULL UNIQUE,
                            password VARCHAR(120) NOT NULL,
                            full_name VARCHAR(160) NOT NULL,
                            role VARCHAR(30) NOT NULL
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS patients (
                            id IDENTITY PRIMARY KEY,
                            first_name VARCHAR(100) NOT NULL,
                            last_name VARCHAR(100) NOT NULL,
                            dni VARCHAR(30) NOT NULL UNIQUE,
                            insurance VARCHAR(120),
                            diagnosis VARCHAR(220),
                            protocol VARCHAR(60) NOT NULL,
                            allergies VARCHAR(300),
                            emergency_contact VARCHAR(180),
                            neutropenic BOOLEAN DEFAULT FALSE,
                            fever BOOLEAN DEFAULT FALSE
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS appointments (
                            id IDENTITY PRIMARY KEY,
                            patient_id BIGINT NOT NULL,
                            doctor_id BIGINT NOT NULL,
                            created_by BIGINT NOT NULL,
                            start_time TIMESTAMP NOT NULL,
                            duration_minutes INT NOT NULL,
                            bed_chair INT NOT NULL,
                            CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
                            CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
                            CONSTRAINT fk_appointment_creator FOREIGN KEY (created_by) REFERENCES users(id)
                        )
                        """);
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS app_config (
                            config_key VARCHAR(80) PRIMARY KEY,
                            config_value VARCHAR(120) NOT NULL
                        )
                        """);
                seed(statement);
            }
        } catch (Exception e) {
            throw new DataAccessException("No se pudo inicializar la base de datos", e);
        }
    }

    private static void seed(Statement statement) throws SQLException {
        statement.executeUpdate("""
                MERGE INTO users (username, password, full_name, role) KEY(username)
                VALUES ('enfermera', '1234', 'Lic. Enfermeria', 'ENFERMERIA')
                """);
        statement.executeUpdate("""
                MERGE INTO users (username, password, full_name, role) KEY(username)
                VALUES ('drlopez', '1234', 'Dr. Lopez', 'MEDICO')
                """);
        statement.executeUpdate("""
                MERGE INTO app_config (config_key, config_value) KEY(config_key)
                VALUES ('workStart', '08:00')
                """);
        statement.executeUpdate("""
                MERGE INTO app_config (config_key, config_value) KEY(config_key)
                VALUES ('workEnd', '18:00')
                """);
        statement.executeUpdate("""
                MERGE INTO app_config (config_key, config_value) KEY(config_key)
                VALUES ('slotMinutes', '30')
                """);
        statement.executeUpdate("""
                MERGE INTO app_config (config_key, config_value) KEY(config_key)
                VALUES ('bedCount', '4')
                """);
    }
}
