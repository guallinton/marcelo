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
}
