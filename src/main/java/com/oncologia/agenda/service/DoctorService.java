package com.oncologia.agenda.service;

import com.oncologia.agenda.dao.UserDao;
import com.oncologia.agenda.model.Role;
import com.oncologia.agenda.model.User;

import java.util.Locale;

public class DoctorService {
    private final UserDao userDao = new UserDao();

    public User createDoctor(String username, String fullName, String password) {
        if (isBlank(username)) {
            throw new ValidationException("El usuario del medico es obligatorio.");
        }
        if (isBlank(fullName)) {
            throw new ValidationException("El nombre completo del medico es obligatorio.");
        }
        if (isBlank(password)) {
            throw new ValidationException("La contrasena inicial es obligatoria.");
        }
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (userDao.findByUsername(normalizedUsername).isPresent()) {
            throw new ValidationException("Ya existe un usuario con ese nombre de acceso.");
        }
        User doctor = new User();
        doctor.setUsername(normalizedUsername);
        doctor.setFullName(fullName.trim());
        doctor.setPassword(password.trim());
        doctor.setRole(Role.MEDICO);
        return userDao.insert(doctor);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
