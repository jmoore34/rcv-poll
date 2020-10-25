package io.github.jmoore34.repository

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

object Votes: IntIdTable() {
    val ip = binary("ip", 8)
    val poll = reference("poll", Polls)
    val choices = text("choices")
    // array stored as text
    val choicesDelimiter = ","

    val datetime = datetime("datetime")
}

class Vote(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Vote>(Votes)
    var ip by Votes.ip
    var poll by Poll referencedOn Votes.poll
    var choices by Votes.choices
    var datetime by Votes.datetime
}