package org.example.repositories;

import org.example.models.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findById(int id);
    List<User> findAll();
    User save(User user);
}