package com.rx.geminipro.services

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

data class ActionLogEntry(
    val id: Long,
    val timestamp: Long,
    val action: String,
    val details: String,
    val result: String
)

class ActionLogger private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "ActionLogger"
        private const val DATABASE_NAME = "gemini_actions.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "action_log"
        private const val COL_ID = "id"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_ACTION = "action"
        private const val COL_DETAILS = "details"
        private const val COL_RESULT = "result"

        @Volatile
        private var instance: ActionLogger? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = ActionLogger(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): ActionLogger? = instance
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_ACTION TEXT NOT NULL,
                $COL_DETAILS TEXT,
                $COL_RESULT TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun logAction(action: String, details: String, result: String) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COL_TIMESTAMP, System.currentTimeMillis())
                put(COL_ACTION, action)
                put(COL_DETAILS, details)
                put(COL_RESULT, result)
            }
            db.insert(TABLE_NAME, null, values)
            Log.d(TAG, "Logged: $action - $details - $result")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging action: ${e.message}")
        }
    }

    fun getRecentActions(limit: Int = 50): List<ActionLogEntry> {
        val actions = mutableListOf<ActionLogEntry>()
        try {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_NAME, null, null, null, null, null,
                "$COL_TIMESTAMP DESC", limit.toString()
            )
            cursor.use {
                while (it.moveToNext()) {
                    actions.add(ActionLogEntry(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                        action = it.getString(it.getColumnIndexOrThrow(COL_ACTION)),
                        details = it.getString(it.getColumnIndexOrThrow(COL_DETAILS)) ?: "",
                        result = it.getString(it.getColumnIndexOrThrow(COL_RESULT)) ?: ""
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading actions: ${e.message}")
        }
        return actions
    }

    fun clearLog() {
        try {
            writableDatabase.delete(TABLE_NAME, null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing log: ${e.message}")
        }
    }

    fun getActionCount(): Int {
        return try {
            val db = readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
            cursor.use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } catch (e: Exception) {
            0
        }
    }
}
