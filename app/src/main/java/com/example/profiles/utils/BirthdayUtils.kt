package com.example.profiles.utils
import java.time.LocalDate
import java.time.Period

fun ageOf(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Int =
    Period.between(birthDate, today).years