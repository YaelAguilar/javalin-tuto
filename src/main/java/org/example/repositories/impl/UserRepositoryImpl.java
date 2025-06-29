package org.example.repositories.impl;

import org.example.daos.UserDAO;
import org.example.models.User;
import org.example.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {
    private final UserDAO userDAO;

    public UserRepositoryImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    @Override
    public Optional<User> findById(int id) {
        return userDAO.findById(id);
    }

    @Override
    public List<User> findAll() {
        return userDAO.findAll();
    }

    @Override
    public User save(User user) {
        return userDAO.save(user);
    }
}