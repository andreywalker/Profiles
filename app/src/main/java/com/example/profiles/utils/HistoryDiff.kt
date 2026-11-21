package com.example.profiles.utils
import com.example.profiles.R
import com.example.profiles.data.PersonSnapshot
import java.time.LocalDate

data class Change(val label: String, val old: String, val new: String)

fun diffSnapshots(
    before: PersonSnapshot,
    after: PersonSnapshot,
    label: (Int) -> String,
    groupName: (Long) -> String,
    fieldName: (Long) -> String?,
    date: (LocalDate) -> String
): List<Change> {
    val out = mutableListOf<Change>()
    fun add(name: String, old: String?, new: String?) {
        val o = old.orEmpty(); val n = new.orEmpty()
        if (o != n) out += Change(name, o.ifEmpty { "—" }, n.ifEmpty { "—" })
    }
    val b = before.person; val a = after.person

    add(label(R.string.first_name), b.firstName, a.firstName)
    add(label(R.string.surname), b.surname, a.surname)
    add(label(R.string.called_name), b.name, a.name)
    add(label(R.string.birth_date), date(b.birthDate), date(a.birthDate))
    add(label(R.string.group), groupName(b.groupId), groupName(a.groupId))
    add(label(R.string.section_met), b.met, a.met)
    add(label(R.string.section_subtitle), b.subtitle, a.subtitle)
    add(label(R.string.phone), b.phone, a.phone)
    add(label(R.string.telegram), b.telegram, a.telegram)
    add(label(R.string.instagram), b.instagram, a.instagram)
    add(label(R.string.other_media), b.otherMedia, a.otherMedia)
    if (b.photoPath != a.photoPath) {
        out += Change(
            label(R.string.photo),
            label(if (b.photoPath == null) R.string.photo_none else R.string.photo_old),
            label(if (a.photoPath == null) R.string.photo_none else R.string.photo_new)
        )
    }
    for (id in before.values.keys + after.values.keys) {
        val name = fieldName(id) ?: continue          // field was deleted since
        add(name, before.values[id], after.values[id])
    }
    return out
}