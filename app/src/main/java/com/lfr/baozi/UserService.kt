package com.example.open_autoglm_android

import android.os.RemoteException
import kotlin.jvm.Throws
import kotlin.system.exitProcess

class UserService: IUserService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun exit() {
        destroy()
    }

    @Throws(RemoteException::class)
    override fun execLine(command: String?): Int {
        val  process = Runtime.getRuntime().exec(command)
        return process.waitFor()
    }

    @Throws(RemoteException::class)
    override fun execArr(command: Array<out String?>?): Int {
        val  process = Runtime.getRuntime().exec(command)
        return process.waitFor()
    }
}