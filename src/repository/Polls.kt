package io.github.jmoore34.repository

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Polls : IdTable<String>() {
    val slug = varchar("id", 10).uniqueIndex()
    val name = varchar("name", 64)
    val candidates = text("candidates")
    // array, serialized as text
    val candidatesDelimiter = "@@"

    val result = text("options").nullable()
    override val id: Column<EntityID<String>> = slug.entityId()
}

class Poll(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Poll>(Polls)
    var slug by Polls.slug
    var name by Polls.name
    var candidates by Polls.candidates
    var result by Polls.result
    val votes by Vote referrersOn Votes.poll
}


