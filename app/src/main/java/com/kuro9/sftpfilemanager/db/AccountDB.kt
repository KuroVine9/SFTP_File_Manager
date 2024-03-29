package com.kuro9.sftpfilemanager.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kuro9.sftpfilemanager.data.Account

@Database(entities = [Account::class], version = 2, exportSchema = false)
abstract class AccountDB : RoomDatabase() {

    abstract fun accountDao(): AccountDAO

    companion object {
        @Volatile
        private var INSTANCE: AccountDB? = null

        fun getDatabase(context: Context): AccountDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AccountDB::class.java,
                    "account_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}