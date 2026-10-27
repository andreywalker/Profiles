package com.example.profiles.utils
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

fun ageOf(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Int =
    Period.between(birthDate, today).years


fun nextBirthday(birthDate: LocalDate, today: LocalDate = LocalDate.now()): LocalDate {
    val thisYear = birthDate.withYear(today.year)
    return if (thisYear.isBefore(today)) birthDate.withYear(today.year + 1) else thisYear
}

fun daysUntil(date: LocalDate, today: LocalDate = LocalDate.now()): Int =
    ChronoUnit.DAYS.between(today, date).toInt()
