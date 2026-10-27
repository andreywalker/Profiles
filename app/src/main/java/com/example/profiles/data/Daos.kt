package com.example.profiles.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY firstName COLLATE NOCASE")
    fun observeAll(): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id")
    fun observe(id: Long): Flow<Person?>

    @Query("SELECT * FROM persons")
    suspend fun getAll(): List<Person>

    @Insert suspend fun insert(person: Person): Long
    @Update suspend fun update(person: Person)
    @Query("DELETE FROM persons WHERE id = :id") suspend fun delete(id: Long)

    @Query("UPDATE persons SET groupId = :toGroup WHERE groupId = :fromGroup")
    suspend fun moveGroup(fromGroup: Long, toGroup: Long)
}

@Dao
interface GroupDao {
    @Query("SELECT * FROM person_groups ORDER BY isDefault, name COLLATE NOCASE")
    fun observeAll(): Flow<List<Group>>

    @Query("SELECT * FROM person_groups WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): Group

    @Insert suspend fun insert(group: Group): Long
    @Update suspend fun update(group: Group)
    @Query("DELETE FROM person_groups WHERE id = :id AND isDefault = 0")
    suspend fun delete(id: Long)
}

@Dao
interface FieldDao {
    // deletable fields first, Facts (deletable = 0) always last
    @Query("SELECT * FROM field_defs ORDER BY deletable DESC, position")
    fun observeDefs(): Flow<List<FieldDef>>

    @Query("SELECT COALESCE(MAX(position), 0) FROM field_defs")
    suspend fun maxPosition(): Int

    @Insert suspend fun insertDef(def: FieldDef): Long
    @Update suspend fun updateDef(def: FieldDef)
    @Query("DELETE FROM field_defs WHERE id = :id AND deletable = 1")
    suspend fun deleteDef(id: Long)

    @Query("SELECT * FROM field_values WHERE personId = :personId")
    fun observeValues(personId: Long): Flow<List<FieldValue>>

    @Query("SELECT * FROM field_values WHERE personId = :personId")
    suspend fun getValues(personId: Long): List<FieldValue>

    @Upsert suspend fun upsertValues(values: List<FieldValue>)
    @Query("DELETE FROM field_values WHERE personId = :personId AND fieldId = :fieldId")
    suspend fun deleteValue(personId: Long, fieldId: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_log ORDER BY sentAt DESC")
    fun observeAll(): Flow<List<ReminderLog>>

    @Query("SELECT COUNT(*) FROM reminder_log WHERE personId = :p AND year = :y AND daysBefore = :d")
    suspend fun exists(p: Long, y: Int, d: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: ReminderLog)
}

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun observe(key: String): Flow<String?>

    @Upsert suspend fun set(setting: Setting)
}