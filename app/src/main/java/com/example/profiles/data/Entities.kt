package com.example.profiles.data


import androidx.room.*
import java.time.LocalDate

enum class FieldType { TEXT/*,  NUMBER, DATE, BOOLEAN*/ }

@Entity(tableName = "person_groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false          // true only for "No group"
)

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val firstName: String,
    val surname: String,
    val birthDate: LocalDate,
    val groupId: Long,
    val met: String,
    val subtitle: String,
    val photoPath: String? = null,
    val phone: String? = null,
    val telegram: String? = null,
    val instagram: String? = null,
    val otherMedia: String? = null
)

@Entity(tableName = "field_defs")
data class FieldDef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: FieldType = FieldType.TEXT,
    val deletable: Boolean = true,          // Facts = false
    val position: Int = 0,
    val builtInKey: String? = null          // "favorite_color", "wishlist", "facts"
)

@Entity(
    tableName = "field_values",
    primaryKeys = ["personId", "fieldId"],
    foreignKeys = [
        ForeignKey(Person::class, ["id"], ["personId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(FieldDef::class, ["id"], ["fieldId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("fieldId")]
)
data class FieldValue(val personId: Long, val fieldId: Long, val value: String)

@Entity(tableName = "reminder_log", primaryKeys = ["personId", "year", "daysBefore"])
data class ReminderLog(
    val personId: Long,
    val year: Int,
    val daysBefore: Int,
    val title: String,
    val text: String,
    val sentAt: Long
)

@Entity(tableName = "settings")
data class Setting(@PrimaryKey val key: String, val value: String)