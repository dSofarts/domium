package ru.domium.documentservice.repository;

import ru.domium.documentservice.model.DocumentTemplate;
import ru.domium.documentservice.model.StageCode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID> {
  Optional<DocumentTemplate> findByCode(String code);

  @Query("select t from DocumentTemplate t where t.stageCode = :stage and (:projectType is null or t.projectType is null or t.projectType = :projectType)")
  List<DocumentTemplate> findForStageAndProjectType(@Param("stage") StageCode stage, @Param("projectType") String projectType);
}
