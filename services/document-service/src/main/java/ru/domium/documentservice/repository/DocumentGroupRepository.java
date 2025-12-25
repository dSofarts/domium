package ru.domium.documentservice.repository;

import ru.domium.documentservice.model.DocumentGroup;
import ru.domium.documentservice.model.DocumentGroupType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentGroupRepository extends JpaRepository<DocumentGroup, UUID> {
  Optional<DocumentGroup> findByProjectIdAndType(UUID projectId, DocumentGroupType type);
  List<DocumentGroup> findAllByProjectId(UUID projectId);
}
