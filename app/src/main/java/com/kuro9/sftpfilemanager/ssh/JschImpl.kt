package com.kuro9.sftpfilemanager.ssh

import android.util.Log
import com.jcraft.jsch.*
import com.kuro9.sftpfilemanager.data.AccountWithPrvKey
import com.kuro9.sftpfilemanager.data.FileDetail
import java.io.ByteArrayOutputStream

object JschImpl {
    private var _session: Session? = null
    private var _channel_exec: ChannelExec? = null
    private var _channel_sftp: ChannelSftp? = null
    private var _percent: Long = 0
    private var _account: AccountWithPrvKey? = null

    enum class MODE { UPLOAD, DOWNLOAD }

    private fun testConnection(identity: AccountWithPrvKey): Boolean {
        val result = connect(identity)
        disconnect()
        return result
    }

    fun setIdentify(identity: AccountWithPrvKey): Boolean {
        if (testConnection(identity)) {
            _account = identity
            return true
        }
        return false
    }

    fun isCacheAccountExist(): Boolean = _account !== null

    private fun connect(identity: AccountWithPrvKey): Boolean {
        try {
            identity.apply {
                val jsch = JSch()
                if (prvKey !== null && prvKey != "") {
                    jsch.addIdentity(
                        account.name,
                        prvKey.toByteArray(),
                        null,
                        account.key_passphrase?.toByteArray()
                    )
                }
                _session = jsch.getSession(account.name, account.host, account.port)
                if (account.password !== null) _session!!.setPassword(account.password)
                _session!!.setConfig("StrictHostKeyChecking", "no")
                _session!!.connect()
            }
        } catch (e: JSchException) {
            Log.d("JschImpl", e.stackTraceToString())
            e.printStackTrace()
            this._account = null
            return false
        }
        this._account = identity
        return true
    }

    fun command(command: String): String? {
        if (_session === null && (_account?.let { connect(it) } == false)) {
            Log.e("JschImpl", "No Available Session Connections")
            return null
        }

        lateinit var response: String
        try {
            val response_stream = ByteArrayOutputStream()
            _channel_exec = _session!!.openChannel("exec") as ChannelExec
            _channel_exec!!.setCommand(command)
            _channel_exec!!.outputStream = response_stream
            _channel_exec!!.connect()
            while (_channel_exec!!.isConnected) Thread.sleep(100)
            response = response_stream.toString("UTF-8")
        } catch (e: JSchException) {
            Log.e("JschImpl", e.message!!)
            e.printStackTrace()
            return null
        } catch (e: InterruptedException) {
            Log.e("JschImpl", e.message!!)
            e.printStackTrace()
            return null
        } finally {
            disconnect()
        }
        return response
    }

    fun fileListToDataClassList(command: String): List<FileDetail> {
        val commandResult = this@JschImpl.command(command)
        Log.d("Jsch", "result = $commandResult")
        if (commandResult === null || commandResult == "") return mutableListOf()
        val lines = commandResult.split("\n")
        Log.d("Jsch", "lines = ${lines.size}")
        val fileLines = lines.slice(1..lines.size - 2)
        Log.d("Jsch", "fileLines = $fileLines")

        val result = fileLines.map {
            var details = it.split("[\\s|\t]+".toRegex())
            details = if (details[0] == "") details.slice(1 until details.size) else details
            Log.d("Jsch", "detail = $details")
            FileDetail(
                isDirectory = details[1][0] == 'd',
                fileName = details[9],
                date = "${details[6]} ${details[7]} ${details[8]}",
                author = details[3]
            )
        }

        Log.d("Jsch", result.toString())
        return result
    }

    fun moveFile(source_path: String, destination_path: String, mode: MODE): Boolean {
        _percent = 0 //초기화
        if (_session === null && (_account?.let { connect(it) } == false)) {
            Log.e("JschImpl", "No Available Session Connections")
            return false
        }
        try {
            _channel_sftp = _session!!.openChannel("sftp") as ChannelSftp
            _channel_sftp!!.connect()
            when (mode) {
                MODE.UPLOAD ->
                    _channel_sftp!!.put(source_path, destination_path, SystemOutProgressMonitor)

                MODE.DOWNLOAD ->
                    _channel_sftp!!.get(source_path, destination_path, SystemOutProgressMonitor)
            }
        } catch (e: JSchException) {
            Log.e("JschImpl", e.message!!)
            e.printStackTrace()
        } catch (e: SftpException) {
            Log.e("JschImpl", e.message!!)
            e.printStackTrace()
        } finally {
            disconnect()
        }
        return true
    }

    private fun disconnect() {
        _session?.disconnect()
        _session = null
        _channel_exec?.disconnect()
        _channel_exec = null
        _channel_sftp?.disconnect()
        _channel_sftp = null
    }

    val sendPercent: Long
        get() {
            if (_percent >= 100) {
                _percent = 0
                return 100L
            }
            return _percent
        }

    internal object SystemOutProgressMonitor : SftpProgressMonitor {
        private var fileSize: Long = 0
        private var sendFileSize: Long = 0
        override fun init(op: Int, src: String, dest: String, max: Long) {
            fileSize = max
            Log.d("JschImpl", "Starting : $op  $src -> $dest total : $max")
        }

        override fun count(count: Long): Boolean {
            sendFileSize += count
            val p = sendFileSize * 100 / fileSize
            if (p > _percent) _percent++
            return true
        }

        override fun end() {
            Log.d("JschImpl", "File Transfer Finished")
        }
    }
}