package com.qarzdaftari.aslbek.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.qarzdaftari.aslbek.data.model.Debt
import com.qarzdaftari.aslbek.data.model.User
import java.security.MessageDigest
import java.util.UUID

class DebtDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "qarzdaftari.db"
        private const val DATABASE_VERSION = 1

        // Users table
        private const val TABLE_USERS = "users"
        private const val COL_USER_ID = "id"
        private const val COL_USER_NAME = "name"
        private const val COL_USER_EMAIL = "email"
        private const val COL_USER_PASSWORD = "password_hash"
        private const val COL_USER_CREATED_AT = "created_at"

        // Debts table
        private const val TABLE_DEBTS = "debts"
        private const val COL_DEBT_ID = "id"
        private const val COL_DEBT_USER_ID = "user_id"
        private const val COL_DEBT_NAME = "name"
        private const val COL_DEBT_AMOUNT = "amount"
        private const val COL_DEBT_RETURNED = "returned"
        private const val COL_DEBT_DIRECTION = "direction"
        private const val COL_DEBT_PHONE = "phone"
        private const val COL_DEBT_NOTE = "note"
        private const val COL_DEBT_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID TEXT PRIMARY KEY,
                $COL_USER_NAME TEXT NOT NULL,
                $COL_USER_EMAIL TEXT UNIQUE NOT NULL,
                $COL_USER_PASSWORD TEXT NOT NULL,
                $COL_USER_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()

        val createDebtsTable = """
            CREATE TABLE $TABLE_DEBTS (
                $COL_DEBT_ID TEXT PRIMARY KEY,
                $COL_DEBT_USER_ID TEXT NOT NULL,
                $COL_DEBT_NAME TEXT NOT NULL,
                $COL_DEBT_AMOUNT REAL NOT NULL,
                $COL_DEBT_RETURNED REAL NOT NULL DEFAULT 0,
                $COL_DEBT_DIRECTION TEXT NOT NULL,
                $COL_DEBT_PHONE TEXT,
                $COL_DEBT_NOTE TEXT,
                $COL_DEBT_CREATED_AT INTEGER NOT NULL,
                FOREIGN KEY($COL_DEBT_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createDebtsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DEBTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Auth methods
    fun registerUser(name: String, email: String, password: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()

        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Ismni kiriting"))
        if (cleanEmail.isBlank()) return Result.failure(IllegalArgumentException("Emailni kiriting"))
        if (password.length < 4) return Result.failure(IllegalArgumentException("Parol kamida 4 belgidan iborat bo'lishi kerak"))

        val db = writableDatabase
        return try {
            val cursor = db.query(
                TABLE_USERS,
                arrayOf(COL_USER_ID),
                "$COL_USER_EMAIL = ?",
                arrayOf(cleanEmail),
                null, null, null
            )
            val exists = cursor.use { it.moveToFirst() }
            if (exists) {
                return Result.failure(IllegalArgumentException("Ushbu email bilan foydalanuvchi allaqachon ro'yxatdan o'tgan"))
            }

            val userId = UUID.randomUUID().toString()
            val values = ContentValues().apply {
                put(COL_USER_ID, userId)
                put(COL_USER_NAME, cleanName)
                put(COL_USER_EMAIL, cleanEmail)
                put(COL_USER_PASSWORD, hashPassword(password))
                put(COL_USER_CREATED_AT, System.currentTimeMillis())
            }

            val rowId = db.insert(TABLE_USERS, null, values)
            if (rowId != -1L) {
                Result.success(User(id = userId, name = cleanName, email = cleanEmail))
            } else {
                Result.failure(Exception("Ro'yxatdan o'tishda xatolik yuz berdi"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loginUser(email: String, password: String): Result<User> {
        val cleanEmail = email.trim().lowercase()
        val hashedPassword = hashPassword(password)

        val db = readableDatabase
        return try {
            val cursor = db.query(
                TABLE_USERS,
                arrayOf(COL_USER_ID, COL_USER_NAME, COL_USER_EMAIL),
                "$COL_USER_EMAIL = ? AND $COL_USER_PASSWORD = ?",
                arrayOf(cleanEmail, hashedPassword),
                null, null, null
            )

            cursor.use {
                if (it.moveToFirst()) {
                    val id = it.getString(it.getColumnIndexOrThrow(COL_USER_ID))
                    val name = it.getString(it.getColumnIndexOrThrow(COL_USER_NAME))
                    val userEmail = it.getString(it.getColumnIndexOrThrow(COL_USER_EMAIL))
                    Result.success(User(id = id, name = name, email = userEmail))
                } else {
                    Result.failure(IllegalArgumentException("Email yoki parol noto'g'ri"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Debt saving & listing
    fun insertDebt(debt: Debt): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_DEBT_ID, debt.id)
            put(COL_DEBT_USER_ID, debt.userId)
            put(COL_DEBT_NAME, debt.name.trim())
            put(COL_DEBT_AMOUNT, debt.amount)
            put(COL_DEBT_RETURNED, debt.returned)
            put(COL_DEBT_DIRECTION, debt.direction)
            put(COL_DEBT_PHONE, debt.phone?.trim())
            put(COL_DEBT_NOTE, debt.note?.trim())
            put(COL_DEBT_CREATED_AT, debt.createdAt)
        }
        val rowId = db.insertWithOnConflict(TABLE_DEBTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return rowId != -1L
    }

    fun getDebtsForUser(userId: String): List<Debt> {
        val list = mutableListOf<Debt>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DEBTS,
            null,
            "$COL_DEBT_USER_ID = ?",
            arrayOf(userId),
            null,
            null,
            "$COL_DEBT_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Debt(
                        id = it.getString(it.getColumnIndexOrThrow(COL_DEBT_ID)),
                        userId = it.getString(it.getColumnIndexOrThrow(COL_DEBT_USER_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COL_DEBT_NAME)),
                        amount = it.getDouble(it.getColumnIndexOrThrow(COL_DEBT_AMOUNT)),
                        returned = it.getDouble(it.getColumnIndexOrThrow(COL_DEBT_RETURNED)),
                        direction = it.getString(it.getColumnIndexOrThrow(COL_DEBT_DIRECTION)),
                        phone = it.getString(it.getColumnIndexOrThrow(COL_DEBT_PHONE)),
                        note = it.getString(it.getColumnIndexOrThrow(COL_DEBT_NOTE)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_DEBT_CREATED_AT))
                    )
                )
            }
        }
        return list
    }

    // Debt updating / payment recording
    fun recordPayment(debtId: String, additionalReturned: Double): Boolean {
        val db = writableDatabase
        return try {
            val cursor = db.query(
                TABLE_DEBTS,
                arrayOf(COL_DEBT_AMOUNT, COL_DEBT_RETURNED),
                "$COL_DEBT_ID = ?",
                arrayOf(debtId),
                null, null, null
            )

            var currentAmount = 0.0
            var currentReturned = 0.0
            var found = false

            cursor.use {
                if (it.moveToFirst()) {
                    currentAmount = it.getDouble(it.getColumnIndexOrThrow(COL_DEBT_AMOUNT))
                    currentReturned = it.getDouble(it.getColumnIndexOrThrow(COL_DEBT_RETURNED))
                    found = true
                }
            }

            if (!found) return false

            val newReturned = (currentReturned + additionalReturned).coerceAtMost(currentAmount)
            val values = ContentValues().apply {
                put(COL_DEBT_RETURNED, newReturned)
            }

            val affected = db.update(TABLE_DEBTS, values, "$COL_DEBT_ID = ?", arrayOf(debtId))
            affected > 0
        } catch (e: Exception) {
            false
        }
    }

    // Debt deleting system
    fun deleteDebt(debtId: String): Boolean {
        val db = writableDatabase
        val count = db.delete(TABLE_DEBTS, "$COL_DEBT_ID = ?", arrayOf(debtId))
        return count > 0
    }
}
