package com.kuro9.sftpfilemanager.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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
    private lateinit var account: Account
    private var intent_return: String? = null
    private var _binding: FragmentAccountDetailBinding? = null
    private val binding get() = _binding!!

    private fun bindText() {
        binding.apply {
            textinputAccountName.setText(account.name)
            textinputAccountAddress.setText(account.host)
            textinputAccountPort.setText(account.port.toString())
            textinputAccountPassword.setText(account.password)
            textinputAccountKey.setText(intent_return ?: account.key)
            textinputAccountKeypass.setText(account.key_passphrase)
            floatingActionButton.setOnClickListener {
                saveAccount(account)
                val action =
                    AccountDetailFragmentDirections.actionAccountDetailFragmentToAccountListFragment()
                findNavController().navigate(action)
            }
        }
        intent_return = null
    }

    private fun openActivityResultLauncher(): ActivityResultLauncher<Intent> {
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    Log.d("myintent", "${result.data?.data?.path}")
                    account.key = result.data?.data?.path
                    binding.textinputAccountKey.setText(result.data?.data?.path)
                    intent_return = result.data?.data?.path
                } else {
                    Log.e("myintent", "fil")
                }
            }
        return resultLauncher
    }

    private fun errorToastMessage() {
        Log.e("acc_d", "errtoast")
        TODO("Not yet implemented")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountDetailBinding.inflate(inflater, container, false)
        val activityLauncher = openActivityResultLauncher()
        binding.button2.setOnClickListener {
            val intent: Intent = Intent()
            intent.type = "*/*"
            intent.action = Intent.ACTION_GET_CONTENT
            activityLauncher.launch(intent)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState !== null) {
            savedInstanceState.apply {
                account = Account(
                    id = getInt("id"),
                    name = getString("name").toString(),
                    key = getString("key"),
                    key_passphrase = getString("key_passphrase"),
                    host = getString("host").toString(),
                    port = getInt("port"),
                    password = getString("password")
                )
            }

            bindText()
        } else {
            arguments?.let {
                val id = it.getInt("id")
                if (id < 0) {
                    errorToastMessage()
                    val action =
                        AccountDetailFragmentDirections.actionAccountDetailFragmentToAccountListFragment()
                    findNavController().navigate(action)
                } else {
                    viewModel.getAccount(id).observe(this.viewLifecycleOwner) { acc ->
                        account = acc
                        bindText()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.apply {
            account.name = textinputAccountName.text.toString()
            account.key = textinputAccountKey.text.toString()
            account.key_passphrase = textinputAccountKeypass.text.toString()
            account.host = textinputAccountAddress.text.toString()
            account.port = textinputAccountPort.text.toString().toInt()
            account.password = textinputAccountPassword.text.toString()
        }
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            binding.apply {
                putInt("id", account.id)
                putString("name", textinputAccountName.text.toString())
                putString("key", textinputAccountKey.text.toString())
                putString("key_passphrase", textinputAccountKeypass.text.toString())
                putString("host", textinputAccountAddress.text.toString())
                putInt("port", textinputAccountPort.text.toString().toInt())
                putString("password", textinputAccountPassword.text.toString())
            }
        }
        super.onSaveInstanceState(outState)
    }

    private fun saveAccount(account: Account) {
        binding.apply {
            viewModel.updateAccount(
                account.copy(
                    name = textinputAccountName.text.toString(),
                    key = textinputAccountKey.text.toString(),
                    key_passphrase = textinputAccountKeypass.text.toString(),
                    host = textinputAccountAddress.text.toString(),
                    port = textinputAccountPort.text.toString().toInt(),
                    password = textinputAccountPassword.text.toString()
                )
            )
        }
    }
}