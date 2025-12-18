package ru.domium.documentservice.repository;

import ru.domium.documentservice.model.DocumentAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentAuditLogRepository extends JpaRepository<DocumentAuditLog, UUID> {
  List<DocumentAuditLog> findAllByDocument_IdOrderByCreatedAtAsc(UUID documentId);
}
