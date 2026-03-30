package com.example.kotlin_asr_with_ncnn.core.startup

fun interface StartupTaskProvider {
    fun provide(): List<StartupTask>
}
