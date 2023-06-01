package com.kuro9.sftpfilemanager.fragment

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.kuro9.sftpfilemanager.application.AccountApplication
import com.kuro9.sftpfilemanager.data.Account
import com.kuro9.sftpfilemanager.databinding.FragmentAddAccountBinding
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModel
import com.kuro9.sftpfilemanager.viewmodel.AccountViewModelFactory

/**
 * Fragment to add or update an item in the Inventory database.
 */
class AddAccountFragment : Fragment() {

    // Use the 'by activityViewModels()' Kotlin property delegate from the fragment-ktx artifact
    // to share the ViewModel across fragments.
    private val viewModel: AccountViewModel by activityViewModels {
        AccountViewModelFactory(
            (activity?.application as AccountApplication).database
                .accountDao()
        )
    }

    lateinit var account: Account

    // Binding object instance corresponding to the fragment_add_item.xml layout
    // This property is non-null between the onCreateView() and onDestroyView() lifecycle callbacks,
    // when the view hierarchy is attached to the fragment
    private var _binding: FragmentAddAccountBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Returns true if the EditTexts are not empty
     */
    private fun isValidAccount(): Boolean {

        return viewModel.isValidAccount(
            binding.accountName.text.toString(),
            binding.accountAddress.text.toString(),
            binding.accountPorts.text.toString()
        )
    }

    /**
     * Binds views with the passed in [account] information.
     */
    private fun bind(account: Account) {
        binding.apply {
            accountName.setText(account.name, TextView.BufferType.SPANNABLE)
            accountAddress.setText(account.host, TextView.BufferType.SPANNABLE)
            accountPorts.setText(account.port.toString(), TextView.BufferType.SPANNABLE)
            // saveAction.setOnClickListener { updateAccount() }
        }
    }

    /**
     * Inserts the new Item into database and navigates up to list fragment.
     */
    private fun addNewAccount() {
        if (isValidAccount()) {
            viewModel.addAccount(
                name = binding.accountName.text.toString(),
                host = binding.accountAddress.text.toString(),
                port = binding.accountPorts.text.toString(),
                key = null,
                key_passphrase = null,
                password = null
            )
            val action =
                AddAccountFragmentDirections.actionAddAccountFragmentToAccountListFragment()
            findNavController().navigate(action)
        }
    }

    /**
     * Updates an existing Item in the database and navigates up to list fragment.
     */
//    private fun updateAccount() {
//        if (isValidAccount()) {
//            viewModel.updateAccount(
//                this.navigationArgs.itemId,
//                this.binding.itemName.text.toString(),
//                this.binding.itemPrice.text.toString(),
//                this.binding.itemCount.text.toString()
//            )
//            val action = AddItemFragmentDirections.actionAddItemFragmentToItemListFragment()
//            findNavController().navigate(action)
//        }
//    }

    /**
     * Called when the view is created.
     * The itemId Navigation argument determines the edit item  or add new item.
     * If the itemId is positive, this method retrieves the information from the database and
     * allows the user to update it.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.saveAction.setOnClickListener {
            addNewAccount()
        }
    }

    /**
     * Called before fragment is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Hide keyboard.
        val inputMethodManager = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(requireActivity().currentFocus?.windowToken, 0)
        _binding = null
    }
}
