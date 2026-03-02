package com.liquir

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class LiquirApplication

fun main(args: Array<String>) {
    runApplication<LiquirApplication>(*args)
}
