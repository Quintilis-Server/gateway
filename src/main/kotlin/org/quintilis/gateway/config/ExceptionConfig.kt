package org.quintilis.gateway.config

import org.quintilis.gateway.exception.GlobalErrorAttributes
import org.quintilis.gateway.exception.GlobalErrorWebExceptionHandler
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.codec.ServerCodecConfigurer

@Configuration
class ExceptionConfig {

    @Bean
    @Order(-2)
    fun errorWebExceptionHandler(
        globalErrorAttributes: GlobalErrorAttributes,
        applicationContext: ApplicationContext
    ): GlobalErrorWebExceptionHandler {
        // Cria o configurador de codecs manualmente para evitar erro de autowire
        val serverCodecConfigurer = ServerCodecConfigurer.create()

        return GlobalErrorWebExceptionHandler(
            globalErrorAttributes,
            applicationContext,
            serverCodecConfigurer
        )
    }
}
