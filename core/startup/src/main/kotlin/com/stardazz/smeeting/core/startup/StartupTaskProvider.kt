package com.stardazz.smeeting.core.startup

fun interface StartupTaskProvider {
    fun provide(): List<StartupTask>
}
