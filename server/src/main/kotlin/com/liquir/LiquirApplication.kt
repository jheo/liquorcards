package com.liquir

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LiquirApplication

fun main(args: Array<String>) {
    runApplication<LiquirApplication>(*args)
}
