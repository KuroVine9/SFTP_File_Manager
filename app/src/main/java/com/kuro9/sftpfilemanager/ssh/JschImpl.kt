package com.kuro9.sftpfilemanager.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
import com.kuro9.sftpfilemanager.data.Account
import java.io.ByteArrayOutputStream

object JschImpl {
    private var _session: Session? = null
    private var _channel_exec: ChannelExec? = null
    private var _channel_sftp: ChannelSftp? = null
    private var _percent: Long = 0
    private var _account: Account? = null

    enum class MODE { UPLOAD, DOWNLOAD }

    fun testConnection(account: Account): Boolean {
        val result = connect(account)
        disconnect()
        return result
    }

    fun setIdentify(account: Account): Boolean {
        if (testConnection(account)) {
            _account = account
            return true
        }
        return false
    }

    private fun connect(account: Account): Boolean {
        try {
            account.apply {
                val jsch = JSch()
                // if (key !== null && key != "") jsch.addIdentity(key, key_passphrase)
                if (key !== null && key != "") jsch.addIdentity(
                    name,
                    key!!.toByteArray(),
                    null,
                    key_passphrase?.toByteArray()
                )
                _session = jsch.getSession(name, host, port)
                if (password !== null) _session!!.setPassword(password)
                _session!!.setConfig("StrictHostKeyChecking", "no")
                _session!!.connect()
            }
        } catch (e: JSchException) {
            Log.d("JschImpl", e.stackTraceToString())
            e.printStackTrace()
            this._account = null
            return false
        }
        this._account = account
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