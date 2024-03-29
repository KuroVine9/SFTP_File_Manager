package com.kuro9.sftpfilemanager.viewmodel

import androidx.lifecycle.*
import com.kuro9.sftpfilemanager.data.Account
import com.kuro9.sftpfilemanager.db.AccountDAO
import kotlinx.coroutines.launch

class AccountViewModel(private val accountDAO: AccountDAO) : ViewModel() {

    val allAccounts: LiveData<List<Account>> = accountDAO.getAllAccount().asLiveData()

    fun updateAccount(
        id: Int,
        name: String,
        key: String?,
        key_passphrase: String?,
        host: String,
        port: String,
        password: String?
    ) {
        viewModelScope.launch {
            accountDAO.update(
                Account(id, name, key, key_passphrase, host, port.toInt(), password)
            )
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            accountDAO.update(account)
        }
    }

    fun addAccount(
        name: String,
        key: String?,
        key_passphrase: String?,
        host: String,
        port: String,
        password: String?
    ) {
        viewModelScope.launch {
            accountDAO.insert(
                Account(
                    name = name,
                    key_path = key,
                    key_passphrase = key_passphrase,
                    host = host,
                    port = port.toInt(),
                    password = password
                )
            )
        }
    }

    fun addAccount(account: Account) {
        viewModelScope.launch {
            accountDAO.insert(account)
        }
    }

    fun deleteAccount(id: Int) {
        viewModelScope.launch {
            accountDAO.delete(id)
        }
    }

    fun isValidAccount(
        name: String,
        host: String,
        port: String
    ): Boolean {
        return !(name.isBlank() || host.isBlank() || port.isBlank())
    }

    fun getAccount(id: Int): LiveData<Account> = accountDAO.getAccount(id).asLiveData()

}

class AccountViewModelFactory(private val accountDAO: AccountDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(accountDAO) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}