package com.kuro9.sftpfilemanager.application

import android.app.Application
import com.kuro9.sftpfilemanager.data.AccountDB

class AccountApplication : Application() {
    val database: AccountDB by lazy { AccountDB.getDatabase(this) }
}