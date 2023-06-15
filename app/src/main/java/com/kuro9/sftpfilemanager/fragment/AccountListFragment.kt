package com.kuro9.sftpfilemanager.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kuro9.sftpfilemanager.AccountListAdapter
import com.kuro9.sftpfilemanager.GridSpacingDecorator
import com.kuro9.sftpfilemanager.R
import com.kuro9.sftpfilemanager.application.AccountApplication
import com.kuro9.sftpfilemanager.databinding.FragmentAccountListBinding
import com.kuro9.sftpfilemanager.ssh.JschImpl
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModel
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                Toast.makeText(context, "로그인 시도중...", Toast.LENGTH_SHORT).show()
                lifecycleScope.launch(Dispatchers.IO) {
                    val result: Boolean = withContext(Dispatchers.IO) {
                        JschImpl.setIdentify(it)
                    }

                    Log.d("Jsch", "$result")
                    Handler(Looper.getMainLooper()).post {
                        if (!result) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("로그인 실패")
                                .setMessage("SSH 서버 로그인에 실패하였습니다.")
                                .setPositiveButton("확인") { _, _ -> }
                                .setCancelable(true)
                                .show()
                        } else {
                            //TODO: 파일 창으로 navigation
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("로그인 성공")
                                .setPositiveButton("확인") { _, _ -> }
                                .setCancelable(true)
                                .show()
                        }
                    }
                }
            },
            onDeleteClicked = {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("!!WARNING!!")
                    .setMessage("Are you sure to delete this account?")
                    .setCancelable(true)
                    .setNegativeButton("No") { _, _ -> }
                    .setPositiveButton("Yes") { _, _ ->
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
}
