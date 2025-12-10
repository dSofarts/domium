package ru.domium.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux
import java.util.TimeZone

@EnableWebFlux
@SpringBootApplication
@ConfigurationPropertiesScan
class ChatServiceApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Moscow"))
    runApplication<ChatServiceApplication>(*args)
}
