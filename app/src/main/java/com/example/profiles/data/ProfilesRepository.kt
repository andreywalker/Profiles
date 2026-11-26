package com.example.profiles.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class ProfilesRepository(private val db: AppDb) {
    private val persons = db.personDao()
    private val groups = db.groupDao()
    private val fields = db.fieldDao()
    private val settings = db.settingDao()
    private val history = db.historyDao()

    suspend fun getPerson(id: Long): Person? = persons.observe(id).first()
    suspend fun getFieldValues(personId: Long): List<FieldValue> = fields.getValues(personId)
    suspend fun defaultGroupId(): Long = groups.getDefault().id
    fun observePeople() = persons.observeAll()
    fun observePerson(id: Long) = persons.observe(id)
    fun observeFieldValues(personId: Long) = fields.observeValues(personId)

    data class DeletedGroup(val group: Group, val memberIds: List<Long>)
    data class DeletedField(val def: FieldDef, val values: List<FieldValue>)

    suspend fun savePerson(person: Person, values: Map<Long, String>): Long = db.withTransaction {
        val before = if (person.id == 0L) null else snapshot(person.id)

        val id = if (person.id == 0L) persons.insert(person)
        else { persons.update(person); person.id }
        values.forEach { (fieldId, value) -> if (value.isBlank()) fields.deleteValue(id, fieldId) }
        fields.upsertValues(
            values.filterValues { it.isNotBlank() }.map { (f, v) -> FieldValue(id, f, v.trim()) }
        )

        val after = snapshot(id)!!
        when {
            before == null -> log(HistoryType.CREATED, null, after)
            before != after -> log(HistoryType.UPDATED, before, after)   // saved without changes → no entry
        }
        id
    }


    suspend fun deletePerson(id: Long): Long? = db.withTransaction {
        val before = snapshot(id) ?: return@withTransaction null
        persons.delete(id)                                   // values removed by CASCADE
        log(HistoryType.DELETED, before, null)
    }




    fun observeHistory(since: Long) = history.observeSince(since)

    suspend fun purgeHistory(olderThan: Long) = history.deleteOlderThan(olderThan)

    suspend fun revert(entryId: Long): Boolean = db.withTransaction {
        val e = history.get(entryId)
        if (e == null || e.revertedAt != null) return@withTransaction false
        when (e.type) {
            HistoryType.CREATED -> persons.delete(e.personId)
            HistoryType.UPDATED, HistoryType.DELETED -> restore(PersonSnapshot.fromJson(e.before!!))
        }
        history.markReverted(e.id, System.currentTimeMillis())
        true
    }

    suspend fun referencedPhotos(): Set<String> {
        val keep = persons.getAll().mapNotNullTo(HashSet()) { it.photoPath }
        history.getAll().forEach { e ->
            listOfNotNull(e.before, e.after).forEach { json ->
                PersonSnapshot.fromJson(json).person.photoPath?.let(keep::add)
            }
        }
        return keep
    }

    private suspend fun snapshot(id: Long): PersonSnapshot? {
        val p = persons.getById(id) ?: return null
        return PersonSnapshot(p, fields.getValues(id).associate { it.fieldId to it.value })
    }

    private suspend fun restore(s: PersonSnapshot) {
        // the group or a custom field may have been deleted since then
        val groupId = if (groups.exists(s.person.groupId)) s.person.groupId else groups.getDefault().id
        persons.upsert(s.person.copy(groupId = groupId))      // re-creates the person if deleted
        fields.deleteAllValues(s.person.id)
        val existing = fields.allDefIds().toSet()
        fields.upsertValues(
            s.values.filterKeys { it in existing }.map { (f, v) -> FieldValue(s.person.id, f, v) }
        )
    }

    private suspend fun log(type: HistoryType, before: PersonSnapshot?, after: PersonSnapshot?): Long {
        val p = (after ?: before)!!.person
        return history.insert(
            HistoryEntry(
                personId = p.id,
                personName = "${p.firstName} ${p.surname}",
                type = type,
                before = before?.toJson(),
                after = after?.toJson(),
                timestamp = System.currentTimeMillis()
            )
        )
    }





    fun observeGroups() = groups.observeAll()
    suspend fun addGroup(name: String) = groups.insert(Group(name = name.trim()))
    suspend fun renameGroup(group: Group, name: String) = groups.update(group.copy(name = name.trim()))
    suspend fun deleteGroup(group: Group): DeletedGroup? {
        if (group.isDefault) return null
        return db.withTransaction {
            val members = persons.idsInGroup(group.id)
            persons.moveGroup(group.id, groups.getDefault().id)
            groups.delete(group.id)
            DeletedGroup(group, members)
        }
    }

    suspend fun restoreGroup(d: DeletedGroup) = db.withTransaction {
        groups.insert(d.group)                                   // same id as before
        persons.moveBack(d.memberIds, groups.getDefault().id, d.group.id)
    }



    fun observeFieldDefs() = fields.observeDefs()
    suspend fun addField(name: String, type: FieldType = FieldType.TEXT) =
        fields.insertDef(FieldDef(name = name.trim(), type = type, position = fields.maxPosition() + 1))
    suspend fun renameField(def: FieldDef, name: String) =
        fields.updateDef(def.copy(name = name.trim(), builtInKey = null))
    suspend fun deleteField(def: FieldDef): DeletedField? {
        if (!def.deletable) return null                          // Facts
        return db.withTransaction {
            val values = fields.valuesForField(def.id)
            fields.deleteDef(def.id)                             // values removed by CASCADE
            DeletedField(def, values)
        }
    }

    suspend fun restoreField(d: DeletedField) = db.withTransaction {
        fields.insertDef(d.def)                                  // same id and position
        val existing = persons.getAll().mapTo(HashSet()) { it.id }
        fields.upsertValues(d.values.filter { it.personId in existing })   // skip people deleted since
    }



    
    fun observeSetting(key: String) = settings.observe(key)
    suspend fun setSetting(key: String, value: String) = settings.set(Setting(key, value))

    fun observeReminders() = db.reminderDao().observeAll()
    suspend fun getAllPeople(): List<Person> = persons.getAll()
    suspend fun reminderSent(personId: Long, birthday: LocalDate, daysBefore: Int): Boolean =
        db.reminderDao().exists(personId, birthday, daysBefore)
    suspend fun logReminder(log: ReminderLog) = db.reminderDao().insert(log)



    fun getSettingBlocking(key: String): String? =
        db.openHelper.readableDatabase
            .query("SELECT value FROM settings WHERE `key` = ?", arrayOf(key))
            .use { if (it.moveToFirst()) it.getString(0) else null }

    suspend fun exportAll(): LocalData = db.withTransaction {
        LocalData(groups.getAll(), fields.getAllDefs(), persons.getAll(), fields.getAllValues())
    }
}

data class LocalData(
    val groups: List<Group>,
    val fields: List<FieldDef>,
    val persons: List<Person>,
    val values: List<FieldValue>
)