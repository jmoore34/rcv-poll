package io.github.jmoore34

import io.github.jmoore34.repository.Poll
import io.github.jmoore34.repository.Polls
import io.github.jmoore34.repository.Vote
import io.github.jmoore34.repository.Votes
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.sessions.*
import io.ktor.gson.*
import io.ktor.features.*
import io.ktor.request.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.InetAddress
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Locations) {
    }

    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(ContentNegotiation) {
        gson {
        }
    }

    install(StatusPages) {

    }

    install(XForwardedHeaderSupport) {

    }

    Database.connect(System.getenv("JDBC_DATABASE_URL"), driver = "org.postgresql.Driver", password = System.getenv("JDBC_DATABASE_PASSWORD"), user = System.getenv("JDBC_DATABASE_USER"))
    transaction {
        create(Polls)
    }

    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }

        get<MyLocation> {
            call.respondText("Location: name=${it.name}, arg1=${it.arg1}, arg2=${it.arg2}")
        }
        // Register nested routes
        get<Type.Edit> {
            call.respondText("Inside $it")
        }
        get<Type.List> {
            call.respondText("Inside $it")
        }

        get("/session/increment") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(count = session.count + 1))
            call.respondText("Counter is ${session.count}. Refresh to increment.")
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to listOf(Pair("1","2"), Pair("A","B"))))
        }

        post("/poll") {
            val pollData = call.receive<NewPoll>()
            var generatedSlug = pollData.id ?: newSlug()

            fun sanitizeOptions(options: List<String>) =
            //TODO: validate options to ensure limit number, size
            transaction {
                while (Poll.findById(generatedSlug) != null) {
                    generatedSlug = newSlug()
                }

                Poll.new(generatedSlug) {
                    name = pollData.name
                    candidates = pollData.candidates
                        .map { it.replace(Polls.candidatesDelimiter, "") }
                        .joinToString(Polls.candidatesDelimiter)
                }

            }
            call.respond("${pollData.name}: $generatedSlug")
        }

        post("/vote") {
            val voteData = call.receive<NewVote>()
            transaction {

                Vote.new {
                    ip = ipAddressToByteArray(call.request.origin.remoteHost)
                    poll = Poll.get(voteData.pollId)
                    choices = voteData.choices.joinToString(Votes.choicesDelimiter)
                    datetime = LocalDateTime.now()
                }
            }

        }

        get("/slug") {
            var generatedSlug = newSlug()
            call.respond(generatedSlug)
        }

        get<GetPoll> {
            var poll: Poll? = null
            transaction {
                poll = Poll.findById(it.id)
            }
            if (poll == null)
                call.respond(HttpStatusCode.NotFound, "")
            else
                call.respondText("${poll!!.id}: ${poll!!.name}")
        }

        // Handles all the other non-matched routes returning a 404 not found.
        route("{...}") {
            handle {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

fun ipAddressToByteArray(ip: String) = InetAddress.getByName(ip).address

class NewPoll(val name: String, val candidates: List<String>, val id: String?)

class NewVote(val pollId: String, val choices: List<Int>)


data class Pair(val first: String, val second: String)

@Location("/location/{name}")
class MyLocation(val name: String, val arg1: Int = 42, val arg2: String = "default")

@Location("/type/{name}") data class Type(val name: String) {
    @Location("/edit")
    data class Edit(val type: Type)

    @Location("/list/{page}")
    data class List(val type: Type, val page: Int)
}

@Location("/poll/create/{name}")
class CreatePoll(val name: String)

@Location("/poll/{id}")
class GetPoll(val id: String)

data class MySession(val count: Int = 0)

fun newSlug(): String {
    // https://stackoverflow.com/questions/46943860/idiomatic-way-to-generate-a-random-alphanumeric-string-in-kotlin
    val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return List(8) { alphabet.random() }.joinToString("")
}

