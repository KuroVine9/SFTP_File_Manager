package com.kuro9.sftpfilemanager.fragment

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kuro9.sftpfilemanager.GridSpacingDecorator
import com.kuro9.sftpfilemanager.R
import com.kuro9.sftpfilemanager.adapter.AccountListAdapter
import com.kuro9.sftpfilemanager.data.AccountWithPrvKey
import com.kuro9.sftpfilemanager.databinding.FragmentAccountListBinding
import com.kuro9.sftpfilemanager.db.AccountApplication
import com.kuro9.sftpfilemanager.ssh.JschImpl
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModel
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader

class AccountListFragment : Fragment() {
    private val viewModel: AccountViewModel by activityViewModels {
        AccountViewModelFactory(
            (activity?.application as AccountApplication).database.accountDao()
        )
    }

    enum class Layout { Linear, Grid }

    private var _binding: FragmentAccountListBinding? = null
    private val binding get() = _binding!!
    private lateinit var layout: Layout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAccountListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val width = with(resources.displayMetrics) { widthPixels / xdpi }
        Log.d("dpi", "$width")
        layout =
            if (width > 4) Layout.Grid
            else Layout.Linear


        val adapter = AccountListAdapter(
            onEditClicked = {
                this.findNavController().navigate(
                    AccountListFragmentDirections.actionAccountListFragmentToAccountDetailFragment(
                        id = it.id
                    )
                )
            },
            onLoginClicked = {
                Toast.makeText(context, R.string.attempting_login, Toast.LENGTH_SHORT).show()
                checkPermission()
                Log.d("Jsch", "${it.key_path}")
                var key_text: String? = null
                try {
                    key_text = readTextFromUri(Uri.parse(it.key_path))
                } catch (_: IOException) {

                } catch (_: FileNotFoundException) {

                }
                lifecycleScope.launch(Dispatchers.IO) {
                    val result: Boolean = withContext(Dispatchers.IO) {
                        JschImpl.setIdentify(AccountWithPrvKey(it, key_text))
                    }

                    val homePath = withContext(Dispatchers.IO) {
                        JschImpl.command("echo \$HOME")?.trimEnd()
                    }

                    Log.d("Jsch", "$result")
                    Handler(Looper.getMainLooper()).post {
                        if (!result) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.login_fail)
                                .setMessage(R.string.login_fail_msg)
                                .setPositiveButton(R.string.positive) { _, _ -> }
                                .setCancelable(true)
                                .show()
                        } else {
                            Log.d("nav", "alert")
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.login_success)
                                .setPositiveButton(R.string.task_continue) { _, _ ->
                                    Log.d("nav", "navigation to filelist")
                                    val action =
                                        AccountListFragmentDirections.actionAccountListFragmentToFileListFragment(
                                            homePath
                                        )
                                    findNavController().navigate(action)
                                }
                                .setCancelable(true)
                                .show()
                        }
                    }
                }
            },
            onDeleteClicked = {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.warning)
                    .setMessage(R.string.warning_to_delete_account)
                    .setCancelable(true)
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .setPositiveButton(R.string.positive) { _, _ ->
                        viewModel.deleteAccount(it.id)
                    }.show()
            }
        )
        binding.accountListRecyclerView.apply {
            if (layout == Layout.Grid) {
                val gridLayoutManager = GridLayoutManager(context, 2)
                layoutManager = gridLayoutManager

                val gridSpacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                val itemDecoration = GridSpacingDecorator(gridSpacing, 2)
                addItemDecoration(itemDecoration)
            } else {
                layoutManager = LinearLayoutManager(context)
            }

            this.adapter = adapter
        }

        viewModel.allAccounts.observe(this.viewLifecycleOwner) { accounts ->
            adapter.submitList(accounts)
        }

        binding.floatingActionButton.setOnClickListener {
            val action =
                AccountListFragmentDirections.actionAccountListFragmentToAccountDetailFragment()
            findNavController().navigate(action)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkPermission() {
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

    @Throws(IOException::class, FileNotFoundException::class)
    private fun readTextFromUri(uri: Uri?): String {
        if (uri === null) return ""

        val stringBuilder = StringBuilder()
        checkPermission()
        activity?.contentResolver?.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append("$line\n")
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }
}
