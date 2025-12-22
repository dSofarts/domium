package ru.domium.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает endpoint как публичный (без обязательной аутентификации).
 * Если paths не заданы, используются пути из @RequestMapping/@GetMapping и т.п.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PublicEndpoint {
    String[] paths() default {};
}

