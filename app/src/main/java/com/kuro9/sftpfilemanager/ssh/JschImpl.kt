package com.kuro9.sftpfilemanager.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.SftpProgressMonitor
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

    /**
     * 계정이 valid한지 확인
     * @param identity 계정
     * @return valid 여부
     */
    private fun testConnection(identity: AccountWithPrvKey): Boolean {
        val result = connect(identity)
        disconnect()
        return result
    }

    /**
     * 클래스에 계정정보 저장
     * @param identity 계정
     * @return 계정이 valid한지 여부
     */
    fun setIdentify(identity: AccountWithPrvKey): Boolean {
        if (testConnection(identity)) {
            _account = identity
            return true
        }
        return false
    }

    /**
     * 서버에 접속 시도
     * @param identity 계정
     * @return 계정이 valid한지 여부
     */
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

    /**
     * 명령어 실행. 반드시 [setIdentify]가 선행되어야 함
     * @param command 명령어
     * @return 출력 값
     */
    fun command(command: String): String? {
        if (_session === null && _account?.let { connect(it) } != true) {
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

    /**
     * [command]를 이용해 경로의 파일을 리스트로 반환
     * @param path 경로
     * @return .와 ..를 제외한 파일의 정보 리스트(에러 시 null)
     */
    fun getFileList(path: String): MutableList<FileDetail>? {
        val commandResult = this@JschImpl.command("ls -al $path")
        Log.d("Jsch", "result = $commandResult")
        if (commandResult === null || commandResult == "") return null
        val lines = commandResult.split('\n')
        Log.d("Jsch", "lines = ${lines.size}")
        val fileLines = lines.slice(3..lines.size - 2)
        Log.d("Jsch", "fileLines = $fileLines")

        val result = fileLines.map {
            val details = it.split("[\\s|\t]+".toRegex())
            Log.d("Jsch", "detail = $details")
            FileDetail(
                isDirectory = details[0][0] == 'd',
                fileName = details.slice(8 until details.size).joinToString(separator = " "),
                date = "${details[5]} ${details[6]} ${details[7]}",
                author = details[2]
            )
        }

        Log.d("Jsch", result.toString())
        return result.toMutableList()
    }

    /**
     * 파일 업로드/다운로드
     * @param source_path 다운로드할 파일의 경로/업로드할 파일의 경로
     * @param destination_path 다운로드될 위치/업로드될 위치
     * @param mode 업로드/다운로드 설정
     * @return 성공 여부
     */
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

    /**
     * 접속 종료
     */
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