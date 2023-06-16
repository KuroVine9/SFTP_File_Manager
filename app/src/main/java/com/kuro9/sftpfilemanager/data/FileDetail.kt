package com.kuro9.sftpfilemanager.data

import java.util.Date

data class FileDetail(
    val isDirectory:Boolean,
    val fileName:String,
    val date:String,
    val author:String
)
