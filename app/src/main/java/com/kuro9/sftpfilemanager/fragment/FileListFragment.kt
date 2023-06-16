package com.kuro9.sftpfilemanager.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kuro9.sftpfilemanager.adapter.FileListAdapter
import com.kuro9.sftpfilemanager.databinding.FragmentFileListBinding
import com.kuro9.sftpfilemanager.ssh.JschImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FileListFragment : Fragment() {
    private var _binding: FragmentFileListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFileListBinding.inflate(inflater, container, false)

        arguments?.let {
            (requireActivity() as AppCompatActivity)
                .supportActionBar?.setDisplayShowTitleEnabled(true)
            (requireActivity() as AppCompatActivity).supportActionBar?.title =
                it.getString("path") ?: "~"
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireArguments().let {
            val path = it.getString("path", "~")
            Log.d("Jsch", "path = $path")
            lifecycleScope.launch(Dispatchers.IO) {
                val dataset = JschImpl.getFileList(path)

                Handler(Looper.getMainLooper()).post {
                    Log.d("Jsch", "adapter set")
                    binding.fileRecyclerView.adapter =
                        FileListAdapter(
                            requireContext(),
                            dataset,
                            { file ->
                                val newPath = path + "/" + file.fileName
                                val action =
                                    FileListFragmentDirections.actionFileListFragmentSelf(newPath)
                                findNavController().navigate(action)
                            },
                            { file ->
                                //TODO: Download
                            }
                        )

                    if (dataset.isEmpty()) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("서버 통신 실패")
                            .setMessage("SSH 서버와의 통신이 실패했습니다. 홈으로 돌아갑니다. ")
                            .setPositiveButton("확인") { _, _ ->
                                val action =
                                    FileListFragmentDirections.actionFileListFragmentToAccountListFragment()
                                findNavController().navigate(action)
                            }
                            .setCancelable(false)
                            .show()
                    }
                }
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}