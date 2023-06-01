package com.kuro9.sftpfilemanager

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kuro9.sftpfilemanager.data.Account
import com.kuro9.sftpfilemanager.databinding.AccountListBinding

class AccountListAdapter(private val onClicked: (Account) -> Unit) :
    ListAdapter<Account, AccountListAdapter.AccountViewHolder>(DiffCallback) {

    class AccountViewHolder(private var binding: AccountListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(account: Account) {
            binding.apply {
                accountName.text = account.name
                accountAddress.text = account.host
                accountPort.text = account.port.toString()
            }
        }
//        val accName: TextView = view.findViewById(R.id.account_name)
//        val accAddress: TextView = view.findViewById(R.id.account_address)
//        val accPort: TextView = view.findViewById(R.id.account_port)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(
            AccountListBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            )
        )
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
//        val account = getItem()
//        holder.accName.text = account.name
//        holder.accAddress.text = account.host
//        holder.accPort.text = account.port.toString()
        val current = getItem(position)
        holder.itemView.setOnClickListener {
            onClicked(current)
        }
        holder.bind(current)
    }


    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Account>() {
            override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean =
                (oldItem.id == newItem.id)

            override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean =
                (oldItem == newItem)
        }
    }
}