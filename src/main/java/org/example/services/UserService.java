package org.example.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.example.dtos.auth.RegisterRequest;
import org.example.dtos.user.UserDTO;
import org.example.exceptions.BadRequestException;
import org.example.exceptions.NotFoundException;
import org.example.mappers.UserMapper;
import org.example.models.Role;
import org.example.models.User;
import org.example.repositories.UserRepository;

import java.util.List;

public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserDTO registerWaiter(RegisterRequest request) {
        validateRegistrationRequest(request);

        if (userRepository.findByEmail(request.email().trim()).isPresent()) {
            throw new BadRequestException("Email '" + request.email() + "' is already in use.");
        }

        User newUser = new User();
        newUser.setFirstName(request.firstName().trim());
        if (request.middleName() != null && !request.middleName().trim().isEmpty()) {
            newUser.setMiddleName(request.middleName().trim());
        }
        newUser.setLastName(request.lastName().trim());
        newUser.setEmail(request.email().trim());
        
        String hashedPassword = BCrypt.withDefaults().hashToString(12, request.password().toCharArray());
        newUser.setPassword(hashedPassword);
        
        newUser.setRole(Role.WAITER);

        User savedUser = userRepository.save(newUser);
        return userMapper.toUserDTO(savedUser);
    }

    public List<UserDTO> findAllUsers() {
        return userMapper.toUserDTOList(userRepository.findAll());
    }

    public UserDTO findUserById(int id) {
        return userRepository.findById(id)
                .map(userMapper::toUserDTO)
                .orElseThrow(() -> new NotFoundException("User with ID " + id + " not found."));
    }

    private void validateRegistrationRequest(RegisterRequest request) {
        if (request.firstName() == null || request.firstName().trim().isEmpty() ||
            request.lastName() == null || request.lastName().trim().isEmpty() ||
            request.email() == null || request.email().trim().isEmpty() ||
            request.password() == null || request.password().isEmpty()) {
            throw new BadRequestException("First name, last name, email, and password are required.");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match.");
        }
    }
}