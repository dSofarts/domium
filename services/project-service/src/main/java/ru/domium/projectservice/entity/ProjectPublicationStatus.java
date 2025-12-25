package ru.domium.projectservice.entity;

public enum ProjectPublicationStatus {
    DRAFT,          // на стадии создания (еще не опубликован)
    PUBLISHED,      // успешно опубликован на сайте
    PUBLISH_FAILED, // неуспешная загрузка
//    DELETED,         // удалён с витрины
//    BANNED          // заблокирован администратором
}
