package com.kuro9.sftpfilemanager.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kuro9.sftpfilemanager.application.AccountApplication
import com.kuro9.sftpfilemanager.data.Account
import com.kuro9.sftpfilemanager.databinding.FragmentAccountDetailBinding
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModel
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModelFactory

class AccountDetailFragment : Fragment() {
    private val viewModel: AccountViewModel by activityViewModels {
        AccountViewModelFactory(
            (activity?.application as AccountApplication).database.accountDao()
        )
    }
    private val account = Account(0, "kurovine9", "qwer1123", null, "127.0.0.1", 22, null)
    private var _binding: FragmentAccountDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.apply {
            textinputAccountName.setText(account.name)
            textinputAccountAddress.setText(account.host)
            textinputAccountPort.setText(account.port.toString())
            floatingActionButton.setOnClickListener {

            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveAccount(account: Account) {

    }
}