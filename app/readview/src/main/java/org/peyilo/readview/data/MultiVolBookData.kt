package org.peyilo.readview.data

class MultiVolBookData: BookData() {
    override val isMultiVol: Boolean = true
    override val chapCount: Int
        get() {
            var count = 0
            // 将每一卷的章节数加起来
            repeat(childCount) {
                val volume = getChild(it + 1) as VolumeData
                count += volume.chapCount
            }
            return count
        }

    // 二分法实现在具有分卷的书中查找指定的章节
    override fun getChap(index: Int): ChapData {
        var lowIndex = 1
        var highIndex = childCount
        var low = (getChild(lowIndex) as VolumeData).range
        var high = (getChild(highIndex) as VolumeData).range
        var target: RangeData = low
        var tarIndex = lowIndex
        while (low.to < high.from) {
            tarIndex = (lowIndex + highIndex) / 2
            target = (getChild(tarIndex) as VolumeData).range
            if (index >= target.from && index <= target.to) {
                break
            } else if (index < target.from) {
                highIndex = tarIndex
                high = target
            } else {
                lowIndex = tarIndex
                low = target
            }
        }
        val vol = getChild(tarIndex) as VolumeData
        return vol.getChap(index - target.from + 1)
    }
}