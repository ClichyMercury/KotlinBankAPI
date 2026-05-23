package com.kotlinbank.db.repositories

import com.kotlinbank.db.tables.UsersTable
import com.kotlinbank.models.User
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

object UserRepository {

    suspend fun findById(id: UUID): User? = newSuspendedTransaction(Dispatchers.IO) {
        UsersTable.select { UsersTable.id eq id }.singleOrNull()?.toUser()
    }

    suspend fun findByEmail(email: String): User? = newSuspendedTransaction(Dispatchers.IO) {
        UsersTable.select { UsersTable.email eq email }.singleOrNull()?.toUser()
    }

    suspend fun findByPseudo(pseudo: String): User? = newSuspendedTransaction(Dispatchers.IO) {
        UsersTable.select { UsersTable.pseudo eq pseudo }.singleOrNull()?.toUser()
    }

    suspend fun create(email: String, pseudo: String, passwordHash: String): User =
        newSuspendedTransaction(Dispatchers.IO) {
            val id = UUID.randomUUID()
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.email] = email
                it[UsersTable.pseudo] = pseudo
                it[UsersTable.passwordHash] = passwordHash
            }
            UsersTable.select { UsersTable.id eq id }.single().toUser()
        }

    private fun ResultRow.toUser(): User = User(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        pseudo = this[UsersTable.pseudo],
        passwordHash = this[UsersTable.passwordHash],
        isActive = this[UsersTable.isActive],
        createdAt = this[UsersTable.createdAt]
    )
}
