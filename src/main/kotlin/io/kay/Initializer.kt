package io.kay

import io.kay.config.Config
import io.kay.model.*
import io.kay.web.Server
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.LocalDate
import org.joda.time.LocalTime

fun main() {
    val config = Config.readDBConfig()
    Database.connect(config.url, user = config.username, password = config.passphrase, driver = config.driver)

    transaction {
        SchemaUtils.drop(WorkParts, Users, Conditions, FreeParts)
        SchemaUtils.create(WorkParts, Users, Conditions, FreeParts)

        val me = User.new {
            email = "me@myself.test"
            role = UserRole.ADMIN
        }

        val someoneElse = User.new {
            email = "other@as.me"
            role = UserRole.USER
        }

        Condition.new {
            user = me
            from = LocalDate(2019, 5, 13).toDateTimeAtStartOfDay()
            to = LocalDate(2019, 12, 31).toDateTimeAtStartOfDay()
            monday = LocalTime(5, 0).toDateTimeToday()
            tuesday = LocalTime(5, 0).toDateTimeToday()
            wednesday = LocalTime(5, 0).toDateTimeToday()
            thursday = LocalTime(5, 0).toDateTimeToday()
            friday = LocalTime(5, 0).toDateTimeToday()
            saturday = LocalTime(0, 0).toDateTimeToday()
            sunday = LocalTime(0, 0).toDateTimeToday()
            initialVacation = 16
            consumedVacation = 3
        }
        Condition.new {
            user = someoneElse
            from = LocalDate(2019, 5, 13).toDateTimeAtStartOfDay()
            to = LocalDate(2019, 12, 31).toDateTimeAtStartOfDay()
            monday = LocalTime(8, 0).toDateTimeToday()
            tuesday = LocalTime(7, 0).toDateTimeToday()
            wednesday = LocalTime(6, 0).toDateTimeToday()
            thursday = LocalTime(5, 0).toDateTimeToday()
            friday = LocalTime(4, 0).toDateTimeToday()
            saturday = LocalTime(3, 0).toDateTimeToday()
            sunday = LocalTime(2, 0).toDateTimeToday()
            initialVacation = 16
            consumedVacation = 3
        }

        FreePart.new {
            day = LocalDate(2019, 7, 12).toDateTimeAtStartOfDay()
            reason = FreeType.VACATION
            user = me
        }

        WorkPart.new {
            day = LocalDate(2019, 7, 26).toDateTimeAtStartOfDay()
            start = LocalTime(9, 0).toDateTimeToday()
            end = LocalTime(12, 0).toDateTimeToday()
            user = me
        }

        WorkPart.new {
            day = LocalDate(2019, 7, 26).toDateTimeAtStartOfDay()
            start = LocalTime(12, 0).toDateTimeToday()
            user = me
        }

        WorkPart.new {
            day = LocalDate(2019, 7, 25).toDateTimeAtStartOfDay()
            start = LocalTime(12, 0).toDateTimeToday()
            end = LocalTime(16, 0).toDateTimeToday()
            user = me
        }

        WorkPart.new {
            day = LocalDate(2019, 7, 26).toDateTimeAtStartOfDay()
            start = LocalTime(9, 0).toDateTimeToday()
            end = LocalTime(12, 0).toDateTimeToday()
            user = someoneElse
        }

        commit()
    }

    Server.createServer().start(wait = true)
}