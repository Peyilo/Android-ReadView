package org.klee.readview.entities

class LineData(var line: String = "",
        var isLast: Boolean = false,                    // 是否为段落的末行
        var isFirst: Boolean = false)                   // 是否为段落的首行