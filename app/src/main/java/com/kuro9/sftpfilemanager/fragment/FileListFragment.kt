package com.kuro9.sftpfilemanager.fragment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kuro9.sftpfilemanager.adapter.FileListAdapter
import com.kuro9.sftpfilemanager.databinding.FragmentFileListBinding
import com.kuro9.sftpfilemanager.ssh.JschImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class FileListFragment : Fragment() {
    private var _binding: FragmentFileListBinding? = null
    private val binding get() = _binding!!
    private lateinit var downloadedFile: File
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.also { url ->
                        Log.d("myintent", url.data.toString())
                        url.data?.let {
                            val fileStream = requireActivity().contentResolver.openOutputStream(it)
                            if (fileStream !== null) {
                                fileStream.write(downloadedFile.readBytes())
                                fileStream.flush()
                                fileStream.close()
                                Toast.makeText(
                                    requireContext(),
                                    "다운로드 완료",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Log.d("myintent", "filestream is NULL")
                            }
                        }
                    }
                } else {
                    Log.e("myintent", "intent create not successful")
                }
            }
    }

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
                                lifecycleScope.launch(Dispatchers.IO) {
                                    checkPermission()
                                    val result: Boolean = withContext(Dispatchers.IO) {
                                        JschImpl.moveFile(
                                            path + "/" + file.fileName,
                                            requireContext().filesDir.path + "/" + file.fileName,
                                            JschImpl.MODE.DOWNLOAD
                                        )
                                    }
                                    Handler(Looper.getMainLooper()).post {
                                        if (result) {
                                            downloadedFile =
                                                File(requireContext().filesDir, file.fileName)

                                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                                            intent.addCategory(Intent.CATEGORY_OPENABLE)
                                            intent.type = "*/*"
                                            intent.putExtra(Intent.EXTRA_TITLE, file.fileName)
                                            resultLauncher.launch(intent)
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "다운로드 에러",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }

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

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun checkPermission() {
        val writePerm = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPerm = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (writePerm != PackageManager.PERMISSION_GRANTED || readPerm != PackageManager.PERMISSION_GRANTED)
        //권한 요청
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
    }

    private fun openActivityResultLauncher(data: File): ActivityResultLauncher<Intent> {
        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.also { url ->
                        Log.d("myintent", url.data.toString())
                        url.data?.let {
                            val fileStream = requireActivity().contentResolver.openOutputStream(it)
                            if (fileStream !== null) {
                                fileStream.write(data.readBytes())
                                fileStream.flush()
                                fileStream.close()
                            } else {
                                Log.d("myintent", "filestream is NULL")
                            }
                        }
                    }
                } else {
                    Log.e("myintent", "intent create not successful")
                }
            }
        return resultLauncher
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}