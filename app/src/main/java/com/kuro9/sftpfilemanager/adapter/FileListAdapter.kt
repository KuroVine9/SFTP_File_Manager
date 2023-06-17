package com.kuro9.sftpfilemanager.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kuro9.sftpfilemanager.R
import com.kuro9.sftpfilemanager.data.FileDetail

class FileListAdapter(
    private val context: Context,
    private val dataset: MutableList<FileDetail>,
    private val onDirClick: (FileDetail) -> Unit,
    private val onFileClick: (FileDetail) -> Unit,
    private val onFileLongClick: (FileDetail) -> Boolean
) : RecyclerView.Adapter<FileListAdapter.FileListViewHolder>() {

    class FileListViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val fileImage: ImageView = view.findViewById(R.id.file_image)
        val fileName: TextView = view.findViewById(R.id.file_name)
        val fileDate: TextView = view.findViewById(R.id.file_date)
        val fileAuthor: TextView = view.findViewById(R.id.file_author)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileListViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.file_list, parent, false)
        return FileListViewHolder(adapterLayout)
    }

    override fun getItemCount() = dataset.size

    override fun onBindViewHolder(holder: FileListViewHolder, position: Int) {
        val item = dataset[position]
        holder.fileName.text = item.fileName
        holder.fileDate.text = item.date
        holder.fileAuthor.text = item.author

        holder.itemView.apply {
            if (item.isDirectory) {
                holder.fileImage.setImageResource(R.drawable.folder_image)
                setOnClickListener { onDirClick(item) }

            } else {
                holder.fileImage.setImageResource(R.drawable.file_image)
                setOnClickListener { onFileClick(item) }
                setOnLongClickListener { onFileLongClick(item) }
            }
        }
    }
}