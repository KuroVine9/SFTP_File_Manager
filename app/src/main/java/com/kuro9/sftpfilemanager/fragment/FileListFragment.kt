package com.kuro9.sftpfilemanager.fragment

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.OpenableColumns
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
import java.io.FileOutputStream


class FileListFragment : Fragment() {
    private var _binding: FragmentFileListBinding? = null
    private val binding get() = _binding!!
    private lateinit var downloadedFile: File
    private var fileToUpload: File? = null
    private lateinit var downloadLauncher: ActivityResultLauncher<Intent>
    private lateinit var uploadLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        downloadLauncher =
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

        uploadLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let {
                        fileToUpload =
                            File(requireContext().filesDir, getFileName(it) ?: "NewFileName")
                        requireActivity().contentResolver.openInputStream(it)?.use { inputStream ->
                            val fileOutputStream = FileOutputStream(fileToUpload)
                            var n = 0
                            fileOutputStream.use { outputStream ->
                                val buffer = ByteArray(1024)
                                do {
                                    n = inputStream.read(buffer)
                                    if (n > 0) outputStream.write(buffer, 0, n)
                                    else break
                                } while (true)
                            }
                        }
                    }

                    if (fileToUpload === null) {
                        Toast.makeText(
                            requireContext(),
                            "파일 불러오기 실패",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@registerForActivityResult
                    }

                    lifecycleScope.launch(Dispatchers.IO) {
                        val result = JschImpl.moveFile(
                            fileToUpload!!.path,
                            requireArguments().getString("path", "~"),
                            JschImpl.MODE.UPLOAD
                        )

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(
                                requireContext(),
                                if (result) "업로드 성공" else "업로드 실패",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        fileToUpload = null
                        createPage()
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

        createPage()

        binding.uploadFloatingButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            uploadLauncher.launch(intent)

        }

        return binding.root
    }

    private fun createPage() {
        requireArguments().let {
            val path = it.getString("path", "~")
            Log.d("Jsch", "path = $path")
            lifecycleScope.launch(Dispatchers.IO) {
                val dataset = JschImpl.getFileList(path)

                Handler(Looper.getMainLooper()).post {
                    if (dataset === null) {
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
                    } else if (dataset.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "표시할 내용이 없습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        binding.fileRecyclerView.adapter =
                            FileListAdapter(
                                requireContext(),
                                dataset,
                                onDirClick = { file ->
                                    val newPath = path + "/" + file.fileName
                                    val action =
                                        FileListFragmentDirections.actionFileListFragmentSelf(
                                            newPath
                                        )
                                    findNavController().navigate(action)
                                },
                                onFileClick = { file ->
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
                                                downloadLauncher.launch(intent)
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
                    }
                }
            }
        }
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

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? =
                requireContext().contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                if (cursor !== null && cursor.moveToFirst()) {
                    val cIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cIndex < 0) return null
                    result = cursor.getString(cIndex)
                }
            }
        }
        if (result === null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}