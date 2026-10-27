package com.example.profiles.notifications

import android.content.Context
import androidx.work.*
import com.example.profiles.ProfilesApp
import com.example.profiles.R
import com.example.profiles.data.ReminderLog
import com.example.profiles.utils.*
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class BirthdayWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = (applicationContext as ProfilesApp).repository
        // notifications in the app's chosen language
        val ctx = LocaleHelper.wrap(applicationContext, repo.getSettingBlocking("language") ?: "system")
        val today = LocalDate.now()
        val wishlistDef = repo.observeFieldDefs().first().firstOrNull { it.builtInKey == "wishlist" }

        for (p in repo.getAllPeople()) {
            val next = nextBirthday(p.birthDate, today)
            val days = daysUntil(next, today)

            // smallest threshold that covers "days": 20 → 30, 10 → 15, 3 → 7, 40 → none.
            // If the phone was off on the exact day, the reminder still comes once.
            val threshold = REMINDER_DAYS.firstOrNull { days <= it } ?: continue
            if (repo.reminderSent(p.id, next.year, threshold)) continue   // no duplicates

            val title = if (days == 0) {
                ctx.getString(R.string.reminder_title_today, p.name)
            } else {
                ctx.getString(R.string.reminder_title, p.name,
                    ctx.resources.getQuantityString(R.plurals.days, days, days))
            }

            val date = next.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
            val lines = mutableListOf(ctx.getString(R.string.reminder_turns, ageOf(p.birthDate, next), date))
            if (p.subtitle.isNotBlank()) lines += p.subtitle
            wishlistDef?.let { def ->
                repo.getFieldValues(p.id).firstOrNull { it.fieldId == def.id }?.value
            }?.let { lines += ctx.getString(R.string.reminder_wishlist, it) }
            val text = lines.joinToString("\n")

            NotificationHelper.show(ctx, p.id, threshold, title, text)
            repo.logReminder(ReminderLog(p.id, next.year, threshold, title, text, System.currentTimeMillis()))
        }
        return Result.success()
    }

    companion object {
        private val REMINDER_DAYS = listOf(7, 15, 30)   // ascending

        /** Daily background check, kept by Android across app restarts and reboots. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BirthdayWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("birthdays", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Extra check right now, e.g. on app start. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<BirthdayWorker>().build())
        }
    }
}