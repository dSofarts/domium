package ru.domium.documentservice.repository;

import ru.domium.documentservice.model.DocumentSignature;
import ru.domium.documentservice.model.ActorType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentSignatureRepository extends JpaRepository<DocumentSignature, UUID> {
  List<DocumentSignature> findAllByDocument_IdOrderBySignedAtAsc(UUID documentId);
  boolean existsByDocument_IdAndSignerType(UUID documentId, ActorType signerType);
}
