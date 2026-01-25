package com.example.myapplication.service.detector

import java.util.LinkedList

class CircularBuffer<T>(private val capacity: Int) : LinkedList<T>() {
    override fun add(element: T): Boolean {
        if (size >= capacity) removeFirst()
        return super.add(element)
    }
}
