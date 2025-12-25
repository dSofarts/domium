package ru.domium.documentservice.repository;

import ru.domium.documentservice.model.DocumentFileVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentFileVersionRepository extends JpaRepository<DocumentFileVersion, UUID> {
  List<DocumentFileVersion> findAllByDocument_IdOrderByVersionDesc(UUID documentId);
  long countByDocument_Id(UUID documentId);
}
