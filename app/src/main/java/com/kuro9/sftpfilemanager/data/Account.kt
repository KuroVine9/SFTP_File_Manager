package com.kuro9.sftpfilemanager.data

data class Account(
    val id: Int,
    var visible_name: String,
    var name: String,
    var key: String?,
    var key_passphrase: String?,
    var host: String,
    var port: Int,
    var password: String?,
    )
