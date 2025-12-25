package ru.domium.building.service.stage.requirement;

import ru.domium.building.model.Building;
import ru.domium.building.model.StageTransition;

import java.util.List;
import java.util.UUID;

/**
 * Расширяемая база для требований к переходу этапа.
 * Сейчас требований нет (переход всегда разрешён), но в будущем можно добавлять реализации:
 * - подписанные документы
 * - проверки статусов/таймеров
 * - внешние зависимости и т.п.
 */
public interface StageTransitionRequirement {
    /**
     * Вернуть список нарушений. Если список пуст — требование выполнено.
     * В будущем можно возвращать несколько нарушений (например, не хватает нескольких документов).
     */
    List<TransitionViolation> validate(Building building, UUID userId, StageTransition transition);
}
