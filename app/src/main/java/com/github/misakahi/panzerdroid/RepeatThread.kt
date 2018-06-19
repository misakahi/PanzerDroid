package com.github.misakahi.panzerdroid

import kotlin.concurrent.thread

abstract class RepeatThread {

    private var isActive = false

    abstract fun running()

    fun start(intervalMillis: Long) {
        isActive = true
        thread {
            while (isActive) {
                running()
                Thread.sleep(intervalMillis)
            }
        }
    }

    fun stop() {
        isActive = false
    }
}