package com.example.profiles.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first

class ProfilesRepository(private val db: AppDb) {
    private val persons = db.personDao()
    private val groups = db.groupDao()
    private val fields = db.fieldDao()
    private val settings = db.settingDao()

    suspend fun getPerson(id: Long): Person? = persons.observe(id).first()
    suspend fun getFieldValues(personId: Long): List<FieldValue> = fields.getValues(personId)
    suspend fun defaultGroupId(): Long = groups.getDefault().id
    fun observePeople() = persons.observeAll()
    fun observePerson(id: Long) = persons.observe(id)
    fun observeFieldValues(personId: Long) = fields.observeValues(personId)

    suspend fun savePerson(person: Person, values: Map<Long, String>): Long =
        db.withTransaction {
            val id = if (person.id == 0L) persons.insert(person)
            else { persons.update(person); person.id }
            values.forEach { (fieldId, value) ->
                if (value.isBlank()) fields.deleteValue(id, fieldId)
            }
            fields.upsertValues(
                values.filterValues { it.isNotBlank() }
                    .map { (fieldId, v) -> FieldValue(id, fieldId, v.trim()) }
            )
            id
        }

    suspend fun deletePerson(id: Long) = persons.delete(id)





    fun observeGroups() = groups.observeAll()
    suspend fun addGroup(name: String) = groups.insert(Group(name = name.trim()))
    suspend fun renameGroup(group: Group, name: String) = groups.update(group.copy(name = name.trim()))
    suspend fun deleteGroup(group: Group) {
        if (group.isDefault) return
        db.withTransaction {
            persons.moveGroup(group.id, groups.getDefault().id)
            groups.delete(group.id)
        }
    }




    fun observeFieldDefs() = fields.observeDefs()
    suspend fun addField(name: String, type: FieldType = FieldType.TEXT) =
        fields.insertDef(FieldDef(name = name.trim(), type = type, position = fields.maxPosition() + 1))
    suspend fun renameField(def: FieldDef, name: String) =
        fields.updateDef(def.copy(name = name.trim(), builtInKey = null))
    suspend fun deleteField(def: FieldDef) = fields.deleteDef(def.id)



    
    fun observeSetting(key: String) = settings.observe(key)
    suspend fun setSetting(key: String, value: String) = settings.set(Setting(key, value))

    fun observeReminders() = db.reminderDao().observeAll()
}