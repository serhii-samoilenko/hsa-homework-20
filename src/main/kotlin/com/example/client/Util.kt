package com.example.client

import java.util.concurrent.atomic.AtomicInteger

fun <E> List<E>.toCircularIterator(): () -> E {
    val index = AtomicInteger(0)
    return {
        val currentIndex = index.getAndIncrement() % this.size
        val result = this[currentIndex]
        index.compareAndSet(this.size, 0)
        result
    }
}
