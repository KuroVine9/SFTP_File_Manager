package com.kuro9.sftpfilemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuro9.sftpfilemanager.data.Account
import com.kuro9.sftpfilemanager.databinding.AccountListBinding
import com.kuro9.sftpfilemanager.fragment.AccountListFragment

class AccountListAdapter(
    private val onLoginClicked: (Account) -> Unit,
    private val onEditClicked: (Account) -> Unit,
    private val onDeleteClicked: (Account) -> Unit,
    private val layout: AccountListFragment.Layout
) :
    ListAdapter<Account, AccountListAdapter.AccountViewHolder>(DiffCallback) {

    inner class AccountViewHolder(private var binding: AccountListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(account: Account) {
            binding.apply {
                accountName.text = account.name
                accountAddressandport.text = String.format("%s:%d", account.host, account.port)
                loginbutton.setOnClickListener { onLoginClicked(account) }
                editbutton.setOnClickListener { onEditClicked(account) }
                deletebutton.setOnClickListener { onDeleteClicked(account) }
            }
        }

    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val layoutResId = when (layout) {
            AccountListFragment.Layout.Grid -> R.layout.fragment_account_list
            AccountListFragment.Layout.Linear -> R.layout.linear_fragment_account_list
        }
        val binding = AccountListBinding.inflate(layoutInflater, parent, false)
        return AccountViewHolder(binding)
//        return AccountViewHolder(
//            AccountListBinding.inflate(
//                LayoutInflater.from(
//                    parent.context
//                )
//            )
//        )
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }


    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Account>() {
            override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean =
                (oldItem.id == newItem.id)

            override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean =
                (oldItem.name == newItem.name)
        }
    }
}