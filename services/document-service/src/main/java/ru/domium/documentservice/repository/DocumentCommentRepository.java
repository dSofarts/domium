package ru.domium.documentservice.repository;

import ru.domium.documentservice.model.DocumentComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentCommentRepository extends JpaRepository<DocumentComment, UUID> {
  List<DocumentComment> findAllByDocument_IdOrderByCreatedAtAsc(UUID documentId);
}
