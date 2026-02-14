package org.quintilis.gateway.exception

import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.webflux.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Mono

// Removemos @Component para configurar manualmente
class GlobalErrorWebExceptionHandler(
    errorAttributes: GlobalErrorAttributes,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer
) : AbstractErrorWebExceptionHandler(
    errorAttributes,
    WebProperties.Resources(),
    applicationContext
) {

    init {
        super.setMessageWriters(serverCodecConfigurer.writers)
        super.setMessageReaders(serverCodecConfigurer.readers)
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse)
    }

    private fun renderErrorResponse(request: ServerRequest): Mono<ServerResponse> {
        val errorPropertiesMap = getErrorAttributes(request, ErrorAttributeOptions.defaults())
        
        val status = try {
            val code = errorPropertiesMap["errorCode"]?.toString()?.toIntOrNull() 
            if (code != null && code >= 100 && code < 600) code else 500
        } catch (e: Exception) {
            500
        }

        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorPropertiesMap))
    }
}
