package com.github.misakahi.panzerdroid

import org.junit.Test

import org.junit.Assert.*

class RepeatThreadTest {

    @Test
    fun running() {
        var count = 0
        val th = object : RepeatThread() {
            override fun running() {
                count++
                println(count)
                if (count >= 3)
                    stop()  // stop myself
            }
        }
        th.start(10)
        Thread.sleep(100)   // Wait long enough to complete repeating
        assertEquals(3, count)
    }
}