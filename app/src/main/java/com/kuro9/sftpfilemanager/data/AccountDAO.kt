package com.kuro9.sftpfilemanager.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDAO {

    @Query("SELECT * FROM account")
    fun getAllAccount(): Flow<List<Account>>

    @Query("SELECT * FROM account WHERE id = :id")
    fun getAccount(id: Int): Flow<Account>

    @Insert
    suspend fun insert(account: Account)

    @Update
    suspend fun update(account: Account)

    @Query("DELETE FROM account WHERE id = :id")
    suspend fun delete(id: Int)
}