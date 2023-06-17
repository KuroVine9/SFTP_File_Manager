package com.kuro9.sftpfilemanager.db

import android.app.Application

class AccountApplication : Application() {
    val database: AccountDB by lazy { AccountDB.getDatabase(this) }
}