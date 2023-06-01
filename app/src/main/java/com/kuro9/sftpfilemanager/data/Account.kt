package com.kuro9.sftpfilemanager.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    var name: String,
    var key: String?,
    var key_passphrase: String?,
    var host: String,
    var port: Int,
    var password: String?,
)