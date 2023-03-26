package ru.quipy.bank.accounts.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handle(e: IllegalArgumentException) = ResponseEntity<String>(e.message, HttpStatus.CONFLICT)

    @ExceptionHandler(IllegalStateException::class)
    fun handle(e: IllegalStateException) = ResponseEntity<String>(e.message, HttpStatus.CONFLICT)

    @ExceptionHandler(Throwable::class)
    fun handle(e: Throwable) = ResponseEntity<String>("Internal error", HttpStatus.INTERNAL_SERVER_ERROR)

}