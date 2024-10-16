package lv.app.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "children")
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User parent;
    @ManyToOne
    private Group group;
    @OneToMany(mappedBy = "child")
    private List<Attendance> attendances = new ArrayList<>();
    private String lastname;
    private String firstname;
}
