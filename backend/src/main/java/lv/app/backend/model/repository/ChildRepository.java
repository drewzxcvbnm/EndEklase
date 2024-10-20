package lv.app.backend.model.repository;

import lv.app.backend.model.Child;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChildRepository extends JpaRepository<Child, Long> {
    List<Child> findByGroupId(Long groupId);
}
