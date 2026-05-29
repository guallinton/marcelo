package com.oncologia.agenda.service;

import com.oncologia.agenda.dao.UserDao;
import com.oncologia.agenda.model.User;

import java.util.Optional;

public class AuthService {
    private final UserDao userDao = new UserDao();

    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userDao.findByCredentials(username.trim(), password.trim());
    }

    public void changePassword(User user, String currentPassword, String newPassword, String confirmation) {
        if (user == null) {
            throw new ValidationException("Sesion invalida.");
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ValidationException("Ingrese la contrasena actual.");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 4) {
            throw new ValidationException("La nueva contrasena debe tener al menos 4 caracteres.");
        }
        if (!newPassword.equals(confirmation)) {
            throw new ValidationException("La confirmacion no coincide.");
        }
        if (login(user.getUsername(), currentPassword).isEmpty()) {
            throw new ValidationException("La contrasena actual no es correcta.");
        }
        userDao.updatePassword(user.getId(), newPassword);
        user.setPassword(newPassword);
    }
}
