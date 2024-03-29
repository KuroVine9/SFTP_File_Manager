package com.kuro9.sftpfilemanager.fragment

import android.app.Activity
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kuro9.sftpfilemanager.R
import com.kuro9.sftpfilemanager.data.Account
import com.kuro9.sftpfilemanager.databinding.FragmentAccountDetailBinding
import com.kuro9.sftpfilemanager.db.AccountApplication
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModel
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModelFactory


class AccountDetailFragment : Fragment() {
    private val viewModel: AccountViewModel by activityViewModels {
        AccountViewModelFactory(
            (activity?.application as AccountApplication).database.accountDao()
        )
    }

    enum class OPMODE { ADD, EDIT }

    private lateinit var account: Account
    private lateinit var mode: OPMODE
    private var intent_return: Uri? = null
    private var _binding: FragmentAccountDetailBinding? = null
    private val binding get() = _binding!!

    private fun bindText() {
        binding.apply {
            textinputAccountName.setText(account.name)
            textinputAccountAddress.setText(account.host)
            textinputAccountPort.setText(account.port.toString())
            textinputAccountPassword.setText(account.password)
            textinputAccountKey.setText(account.key_path)
            textinputAccountKeypass.setText(account.key_passphrase)
        }
        intent_return = null
    }

    private fun openActivityResultLauncher(): ActivityResultLauncher<Intent> {
        /*선택한 파일 경로 가져오기*/
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.also { url ->
                        Log.d("myintent", url.data.toString())
                        intent_return = url.data
                        binding.textinputAccountKey.setText(intent_return.toString())
                        account.key_path = intent_return.toString()
                        url.data?.let {
                            requireActivity().contentResolver.takePersistableUriPermission(
                                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                    }
                } else {
                    Log.e("myintent", "fail to open file")
                }
            }
        return resultLauncher
    }

    private fun checkPermission(view: View) {
        val storagePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (storagePermission != PackageManager.PERMISSION_GRANTED)
        //권한 요청
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                1
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountDetailBinding.inflate(inflater, container, false)
        val activityLauncher = openActivityResultLauncher()

        // 파일 선택 인텐트
        binding.addKey.setOnClickListener {
            checkPermission(it)
            val intent: Intent = Intent(ACTION_OPEN_DOCUMENT)
            intent.type = "application/octet-stream"
            intent.action = ACTION_OPEN_DOCUMENT
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            activityLauncher.launch(intent)
        }

        // 상단바 제목 현재 모드에 따라 변경
        arguments?.let {
            (requireActivity() as AppCompatActivity)
                .supportActionBar?.setDisplayShowTitleEnabled(true)
            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                if (it.getInt("id") < 0) getString(R.string.add_account_frag_title)
                else getString(R.string.edit_account_frag_title)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState !== null) {
            savedInstanceState.apply {
                account = Account(
                    id = getInt("id"),
                    name = getString("name").toString(),
                    key_path = getString("key"),
                    key_passphrase = getString("key_passphrase"),
                    host = getString("host").toString(),
                    port = getInt("port"),
                    password = getString("password")
                )
            }
            bindText()
        } else {
            // 현재 수정/입력인지 판단
            arguments?.let {
                val id = it.getInt("id")
                if (id < 0) {
                    // add모드
                    mode = OPMODE.ADD
                    account = Account(
                        name = "",
                        host = "",
                        port = 22,
                        key_path = null,
                        key_passphrase = "",
                        password = ""
                    )
                } else {
                    // edit모드
                    mode = OPMODE.EDIT
                    viewModel.getAccount(id).observe(this.viewLifecycleOwner) { acc ->
                        account = acc
                        bindText()
                    }
                }
            }
        }

        // 저장버튼
        binding.floatingActionButtonDetailsave.setOnClickListener {
            // 필수항목 중 비어있는 칸 체크
            if (!checkAllEssentialInputValid()) return@setOnClickListener

            account.apply {
                name = binding.textinputAccountName.text.toString()
                key_path = binding.textinputAccountKey.text.toString()
                key_passphrase = binding.textinputAccountKeypass.text.toString()
                host = binding.textinputAccountAddress.text.toString()
                port = binding.textinputAccountPort.text.toString().toInt()
                password = binding.textinputAccountPassword.text.toString()
            }

            if (mode == OPMODE.EDIT) saveAccount(account) else addAccount()
            val action =
                AccountDetailFragmentDirections.actionAccountDetailFragmentToAccountListFragment(id = account.id)
            findNavController().navigate(action)
        }
    }

    // 비어있는 칸 체크
    private fun checkAllEssentialInputValid(): Boolean {
        var result = true
        binding.apply {
            if (textinputAccountName.text.toString().isBlank()) {
                layoutTextinputAccountName.error = "Empty field"
                result = false
            }
            if (textinputAccountAddress.text.toString().isBlank()) {
                layoutTextinputAccountAddress.error = "Empty field"
                result = false
            }
            if (textinputAccountPort.text.toString().isBlank()) {
                layoutTextinputAccountPort.error = "Empty field"
                result = false
            }
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 임시 저장
        binding.apply {
            account.name = textinputAccountName.text.toString()
            account.key_path = textinputAccountKey.text.toString()
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
                    key_path = textinputAccountKey.text.toString(),
                    key_passphrase = textinputAccountKeypass.text.toString(),
                    host = textinputAccountAddress.text.toString(),
                    port = textinputAccountPort.text.toString().toInt(),
                    password = textinputAccountPassword.text.toString()
                )
            )
        }
    }

    private fun addAccount() {
        binding.apply {
            viewModel.addAccount(account)
        }
    }
}