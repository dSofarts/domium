package ru.domium.projectservice.entity;

import lombok.Getter;

@Getter
public enum RoomType {
    BEDROOM("Спальня"),
    BATHROOM("Ванная"),
    KITCHEN("Кухня"),
    LIVING_ROOM("Гостиная"),
    DINING_ROOM("Столовая"),
    OFFICE("Кабинет"),
    GARAGE("Гараж"),
    LAVATORY("Туалет"),
    WARDROBE("Гардероб"),
    OTHER("Другое");

    private final String type;

    RoomType(String type) {
        this.type = type;
    }
}