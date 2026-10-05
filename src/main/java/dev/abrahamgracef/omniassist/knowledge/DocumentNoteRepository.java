package dev.abrahamgracef.omniassist.knowledge;

import dev.abrahamgracef.omniassist.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DocumentNoteRepository extends JpaRepository<DocumentNote, UUID> {

    List<DocumentNote> findByUserOrderByUpdatedAtDesc(User user);

    @Query("SELECT n FROM DocumentNote n WHERE n.user = :user AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(n.tags) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.updatedAt DESC")
    List<DocumentNote> searchByUserAndKeyword(@Param("user") User user, @Param("keyword") String keyword);
}
