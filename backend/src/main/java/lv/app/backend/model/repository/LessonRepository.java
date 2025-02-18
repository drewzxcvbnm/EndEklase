package lv.app.backend.model.repository;

import lv.app.backend.model.Lesson;
import lv.app.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByGroupId(Long groupId);

    @Query("select l.id from Lesson l " +
            "join l.attendances a " +
            "where l.date >= ?1 and l.date <= ?2 and a.child.parent = ?3 and a.invoice is NULL ")
    List<Long> findUserLessonsToPay(LocalDate start, LocalDate end, User user);

    @Query("SELECT DISTINCT l FROM Lesson l " +
            "JOIN l.attendances a " +
            "JOIN a.child c " +
            "WHERE c.parent.id = :userId AND a.status = 'ATTENDED'")
    List<Lesson> findLessonsByUserId(@Param("userId") Long userId);

}
