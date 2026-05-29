package com.oncologia.agenda.dao;

import com.oncologia.agenda.model.ScheduleConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class ConfigDao {
    public ScheduleConfig load() {
        String sql = "SELECT config_key, config_value FROM app_config";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            Map<String, String> values = new HashMap<>();
            while (rs.next()) {
                values.put(rs.getString("config_key"), rs.getString("config_value"));
            }
            ScheduleConfig config = new ScheduleConfig();
            config.setWorkStart(LocalTime.parse(values.getOrDefault("workStart", "08:00")));
            config.setWorkEnd(LocalTime.parse(values.getOrDefault("workEnd", "18:00")));
            config.setSlotMinutes(Integer.parseInt(values.getOrDefault("slotMinutes", "30")));
            config.setBedCount(Integer.parseInt(values.getOrDefault("bedCount", "4")));
            return config;
        } catch (SQLException e) {
            throw new DataAccessException("Error al leer configuracion", e);
        }
    }

    public void save(ScheduleConfig config) {
        upsert("workStart", config.getWorkStart().toString());
        upsert("workEnd", config.getWorkEnd().toString());
        upsert("slotMinutes", String.valueOf(config.getSlotMinutes()));
        upsert("bedCount", String.valueOf(config.getBedCount()));
    }

    private void upsert(String key, String value) {
        String sql = "MERGE INTO app_config (config_key, config_value) KEY(config_key) VALUES (?, ?)";
        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error al guardar configuracion", e);
        }
    }
}
