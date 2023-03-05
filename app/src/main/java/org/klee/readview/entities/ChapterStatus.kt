package org.klee.readview.entities

enum class ChapterStatus {
    NO_LOAD,                // 未加载
    IS_LOADING,             // 加载中
    NO_SPLIT,               // 未分页
    IS_SPLITTING,           // 分页中
    FINISHED                // 分页、加载都已经完成
}