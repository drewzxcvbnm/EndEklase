package lv.app.backend.model;

import jakarta.persistence.*;
import lombok.*;
import lv.app.backend.model.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }


}