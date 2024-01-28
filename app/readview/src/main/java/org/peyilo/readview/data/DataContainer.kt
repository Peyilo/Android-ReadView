package org.peyilo.readview.data
import androidx.annotation.IntRange

/**
 * 标记BookData和VolumeData可以包含ChapData
 */
abstract class DataContainer: AdditionalData() {

    lateinit var range: RangeData

    abstract fun getChap(@IntRange(from = 1) index: Int): ChapData

}
