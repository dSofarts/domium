package ru.domium.documentservice.repository;

import ru.domium.documentservice.model.DocumentInstance;
import ru.domium.documentservice.model.DocumentStatus;
import ru.domium.documentservice.model.DocumentGroupType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentInstanceRepository extends JpaRepository<DocumentInstance, UUID> {

  @Query("select d from DocumentInstance d left join d.group g where d.projectId = :projectId "
      + "and ((:status is null and d.status <> 'DELETE') or d.status = :status) "
      + "and (:stage is null or d.stageCode = :stage) "
      + "and (:groupType is null or g.type = :groupType)")
  List<DocumentInstance> findForProject(@Param("projectId") UUID projectId,
                                       @Param("status") DocumentStatus status,
                                       @Param("stage") UUID stage,
                                       @Param("groupType") DocumentGroupType groupType);

  @Query("select d from DocumentInstance d left join d.group g where d.projectId = :projectId and d.userId = :userId "
      + "and ((:status is null and d.status <> 'DELETE') or d.status = :status) "
      + "and (:stage is null or d.stageCode = :stage) "
      + "and (:groupType is null or g.type = :groupType)")
  List<DocumentInstance> findForProjectAndUser(@Param("projectId") UUID projectId,
                                              @Param("userId") UUID userId,
                                              @Param("status") DocumentStatus status,
                                              @Param("stage") UUID stage,
                                              @Param("groupType") DocumentGroupType groupType);

  List<DocumentInstance> findAllByGroup_Id(UUID groupId);

  @Query("select count(d) from DocumentInstance d where d.projectId = :projectId and d.stageCode = :stage "
      + "and d.template is not null and d.template.required = true "
      + "and d.status <> 'SIGNED' and d.status <> 'DELETE'")
  long countUnsignedRequiredForStage(@Param("projectId") UUID projectId, @Param("stage") UUID stage);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from DocumentInstance d where d.id = :id")
  Optional<DocumentInstance> findByIdForUpdate(@Param("id") UUID id);
}
