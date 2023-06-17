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
import com.kuro9.sftpfilemanager.R
import com.kuro9.sftpfilemanager.adapter.FileListAdapter
import com.kuro9.sftpfilemanager.data.FileDetail
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
    private var dataset: MutableList<FileDetail>? = null
    private lateinit var downloadedFile: File
    private var fileToUpload: File? = null
    private lateinit var downloadLauncher: ActivityResultLauncher<Intent>
    private lateinit var uploadLauncher: ActivityResultLauncher<Intent>

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        // 파일 저장할 경로 선택 후 다운로드
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
                                    R.string.download_complete,
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

        // 업로드할 파일 공유 저장소에서 선택 후 업로드
        uploadLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let {
                        fileToUpload =
                            File(requireContext().filesDir, getFileName(it) ?: "NewFileName")
                        requireActivity().contentResolver.openInputStream(it)?.use { inputStream ->
                            val fileOutputStream = FileOutputStream(fileToUpload)
                            var n: Int
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
                            R.string.file_load_fail,
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
                                if (result) R.string.file_upload_success
                                else R.string.file_load_fail,
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

        // 프래그먼트 내용 생성 및 바인딩
        createPage()

        // 업로드 파일 선택 인텐트 바인딩
        binding.uploadFloatingButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            uploadLauncher.launch(intent)

        }

        return binding.root
    }


    /**
     * 파일 클릭 시 클릭한 파일로 이동(cd)
     * @param file 클릭한 파일. 디렉토리이어야 함
     * @param path 현재 경로
     */
    private fun goInsideDir(file: FileDetail, path: String) {
        val newPath = path + "/" + file.fileName
        val action =
            FileListFragmentDirections.actionFileListFragmentSelf(
                newPath
            )
        findNavController().navigate(action)
    }

    /**
     * 선택한 파일을 다운로드
     * @param file 클릭한 파일. 일반 파일이어야 함
     * @param path 현재 경로
     */
    private fun fileDownload(file: FileDetail, path: String) {
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
                        R.string.download_fail,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * 선택한 파일을 삭제
     * @author kurovine9
     * @param file 클릭한 파일. 일반 파일이어야 함 - 안전을 위해 재귀적 삭제 지원하지 말 것
     * @param path 현재 경로
     * @return 성공 여부
     */
    private fun fileDelete(file: FileDetail, path: String): Boolean {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(file.fileName)
            .setMessage(R.string.warning_to_delete_file)
            .setPositiveButton(R.string.delete) { _, _ ->
                val filePathToDelete = "$path/'${file.fileName}'"
                val fileIndex = dataset!!.indexOf(file)
                Log.d("fileindex", "$fileIndex")
                lifecycleScope.launch(Dispatchers.IO) {
                    val commandResult =
                        JschImpl.command("rm $filePathToDelete && echo 0 || echo -1")

                    val isSuccess =
                        (commandResult !== null) && (commandResult[0] == '0')
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            requireContext(),
                            if (isSuccess) getString(R.string.delete_success)
                            else "${getString(R.string.delete_fail)}: ${
                                commandResult?.split('\n')
                                    ?.get(0) ?: "Unknown Error"
                            }",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dataset!!.clear()
                    val newDataSet = JschImpl.getFileList(path)
                    Handler(Looper.getMainLooper()).post {
                        if (newDataSet === null) {
                            connectErrorAlert()
                        } else {
                            dataset!!.addAll(newDataSet)
                            if (newDataSet.isEmpty()) noDataToast()
                            binding.fileRecyclerView.adapter!!.notifyDataSetChanged()
                        }
                    }

                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setCancelable(true)
            .show()

        return true
    }

    // 프래그먼트 내용 생성 및 바인딩
    private fun createPage() {
        Log.d("createpage", "called")
        requireArguments().let { bundle ->
            val path = bundle.getString("path", "~")
            Log.d("Jsch", "path = $path")
            lifecycleScope.launch(Dispatchers.IO) {
                dataset = JschImpl.getFileList(path)

                if (dataset === null) {
                    connectErrorAlert()
                } else if (dataset!!.isEmpty()) {
                    noDataToast()
                } else {
                    Handler(Looper.getMainLooper()).post {
                        binding.fileRecyclerView.adapter =
                            FileListAdapter(
                                context = requireContext(),
                                dataset = dataset!!,
                                onDirClick = { goInsideDir(it, path) },
                                onFileClick = { fileDownload(it, path) },
                                onFileLongClick = { fileDelete(it, path) }
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

    /**
     * Uri를 이용해 파일의 이름 반환
     * @param uri 파일의 Uri
     * @return 파일의 이름(파싱 실패 시 null)
     */
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

    private fun connectErrorAlert() {
        Handler(Looper.getMainLooper()).post {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.server_connection_fail)
                .setMessage(R.string.server_connection_fail_msg)
                .setPositiveButton(R.string.positive) { _, _ ->
                    val action =
                        FileListFragmentDirections.actionFileListFragmentToAccountListFragment()
                    findNavController().navigate(action)
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun noDataToast() {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(
                requireContext(),
                R.string.no_data_to_display,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}