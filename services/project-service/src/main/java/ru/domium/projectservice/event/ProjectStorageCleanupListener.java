package ru.domium.projectservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.domium.projectservice.objectstorage.service.ImageS3Service;

@Component
@RequiredArgsConstructor
public class ProjectStorageCleanupListener {

    private final ImageS3Service imageS3Service;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ProjectDeletedEvent event) {
        event.imageKeys().forEach(imageS3Service::deleteImageByKey);
    }
}
