package lv.app.backend.mappers;

import lv.app.backend.dto.UserDTO;
import lv.app.backend.model.User;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mapping(target = "password", ignore = true)
    public abstract UserDTO userToDto(User user);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "password", qualifiedByName = "encodePassword")
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "children", ignore = true)
    public abstract void updateUser(@MappingTarget User user, UserDTO userDTO);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    public  abstract User dtoToUser(UserDTO userDTO);


    @Named("encodePassword")
    String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
}