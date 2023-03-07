package org.klee.readview.api

import org.klee.readview.entities.BookData

interface DataSource {
    val book: BookData
}