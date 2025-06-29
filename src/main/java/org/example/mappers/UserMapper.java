package org.example.mappers;

import org.example.dtos.user.UserDTO;
import org.example.models.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {

    /**
     * @param user La entidad de dominio a convertir.
     * @return Un DTO con los datos seguros para el cliente.
     */
    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    /**
     * @param users La lista de entidades de dominio a convertir.
     * @return Una lista de DTOs con los datos seguros.
     */
    public List<UserDTO> toUserDTOList(List<User> users) {
        return users.stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }
}