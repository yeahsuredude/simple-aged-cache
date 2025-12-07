package io.collective

import java.time.Clock

class SimpleAgedKache {

    private val clock: Clock
    private var head: ExpirableEntry? = null

    private class ExpirableEntry(
        var key: Any?,
        var value: Any?,
        var expiresAtMillis: Long,
        var next: ExpirableEntry? = null
    ){
        fun isExpired(nowMillis: Long): Boolean = nowMillis >= expiresAtMillis
    }

    constructor(clock: Clock) {
        this.clock = clock
    }

    constructor() : this(Clock.systemUTC())

    private fun cleanup(){
        val now = clock.millis()
        while (head != null && head!!.isExpired(now)){
            head = head!!.next
        }

        var previous = head
        var current = head?.next
        while (current != null){
            if (current.isExpired(now)){
                previous!!.next = current.next
                current = previous.next
            } else {
                previous = current
                current = current.next
            }
        }
    }

    fun put(key: Any?, value: Any?, retentionInMillis: Int) {
        cleanup()
        val expiresAt = clock.millis() + retentionInMillis
        var current = head
        while (current != null){
            val currentKey = current.key
            if ((currentKey == null && key == null) || (currentKey != null && currentKey == key)){
                current.value = value
                current.expiresAtMillis = expiresAt
                return
            }
            current = current.next
        }
        val newEntry = ExpirableEntry(key, value, expiresAt, head)
        head = newEntry
    }

    fun isEmpty(): Boolean {
        cleanup()
        return head == null
    }

    fun size(): Int {
        cleanup()
        var count = 0
        var current = head
        while (current != null){
            count++
            current = current.next
        }
        return count
    }

    fun get(key: Any?): Any? {
        cleanup()
        var current = head
        while (current != null){
            val currentKey = current.key
            if ((currentKey == null && key == null) || (currentKey != null && currentKey == key)){
                return current.value
            }
            current = current.next
        }
        return null
    }
}