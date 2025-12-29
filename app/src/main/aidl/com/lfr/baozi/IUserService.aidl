package com.example.open_autoglm_android;

// Declare any non-default types here with import statements

interface IUserService {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void exit() = 1; // Exit method defined by user

    /**
     * 执行命令
     */
    int execLine(String command) = 2;

    /**
     * 执行数组中分离的命令
     */
    int execArr(in String[] command) = 3;
}