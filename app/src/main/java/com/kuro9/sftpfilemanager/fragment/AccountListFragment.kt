package com.kuro9.sftpfilemanager.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kuro9.sftpfilemanager.AccountListAdapter
import com.kuro9.sftpfilemanager.application.AccountApplication
import com.kuro9.sftpfilemanager.databinding.FragmentAccountListBinding
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModel
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModelFactory

class AccountListFragment : Fragment() {
    private val viewModel: AccountViewModel by activityViewModels {
        AccountViewModelFactory(
            (activity?.application as AccountApplication).database.accountDao()
        )
    }

    private var _binding: FragmentAccountListBinding? = null
    private val binding get() = _binding!!

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

        val adapter = AccountListAdapter(
            onEditClicked = {
                this.findNavController().navigate(
                    AccountListFragmentDirections.actionAccountListFragmentToAccountDetailFragment(
                        id = it.id
                    )
                )
            },
            onCardClicked = {// TODO: 로그인으로 바꾸기
                this.findNavController().navigate(
                    AccountListFragmentDirections.actionAccountListFragmentToAccountDetailFragment(
                        id = it.id
                    )
                )
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
            // layoutManager = LinearLayoutManager(this@AccountListFragment.context)
            this.adapter = adapter
        }

        viewModel.allAccounts.observe(this.viewLifecycleOwner) { accounts ->
            adapter.submitList(accounts)
        }

        binding.floatingActionButton.setOnClickListener {
//            val action =
//                AccountListFragmentDirections.actionAccountListFragmentToAddAccountFragment()
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
