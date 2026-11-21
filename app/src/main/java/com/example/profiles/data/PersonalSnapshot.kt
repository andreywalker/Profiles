package com.example.profiles.data

import org.json.JSONObject
import java.time.LocalDate

data class PersonSnapshot(val person: Person, val values: Map<Long, String>) {

    fun toJson(): String = JSONObject().apply {
        val p = person
        put("id", p.id)
        put("firstName", p.firstName)
        put("surname", p.surname)
        put("name", p.name)
        put("birthDate", p.birthDate.toString())
        put("groupId", p.groupId)
        put("met", p.met)
        put("subtitle", p.subtitle)
        put("photoPath", p.photoPath ?: JSONObject.NULL)
        put("phone", p.phone ?: JSONObject.NULL)
        put("telegram", p.telegram ?: JSONObject.NULL)
        put("instagram", p.instagram ?: JSONObject.NULL)
        put("otherMedia", p.otherMedia ?: JSONObject.NULL)
        put("values", JSONObject().apply { values.forEach { (k, v) -> put(k.toString(), v) } })
    }.toString()

    companion object {
        fun fromJson(json: String): PersonSnapshot {
            val o = JSONObject(json)
            fun opt(key: String) = if (o.isNull(key)) null else o.getString(key)
            val person = Person(
                id = o.getLong("id"),
                firstName = o.getString("firstName"),
                surname = o.getString("surname"),
                name = o.getString("name"),
                birthDate = LocalDate.parse(o.getString("birthDate")),
                groupId = o.getLong("groupId"),
                met = o.getString("met"),
                subtitle = o.getString("subtitle"),
                photoPath = opt("photoPath"),
                phone = opt("phone"),
                telegram = opt("telegram"),
                instagram = opt("instagram"),
                otherMedia = opt("otherMedia")
            )
            val v = o.getJSONObject("values")
            val values = v.keys().asSequence().associate { it.toLong() to v.getString(it) }
            return PersonSnapshot(person, values)
        }
    }
}