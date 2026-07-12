package ac.roma.npeconnector

import ac.mdiq.podcini.shared.AudioSpec
import ac.mdiq.podcini.shared.VideoSpec
import android.util.Log
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.VideoStream

fun AudioStream.toAudioSpec(): AudioSpec {
    val a = AudioSpec()
    a.averageBitrate = this.averageBitrate
    a.bitrate = this.bitrate
    a.quality = this.quality
    a.codec = this.codec
    a.format = this.format?.name
    a.audioTrackId = this.audioTrackId
    a.audioTrackName = this.audioTrackName
    a.audioLocale = this.audioLocale?.toLanguageTag()
    a.deliveryMethod = this.deliveryMethod.name
    a.url = if (this.isUrl) this.content else {
        Log.e("toAudioSpec", "AudioStream content is not url: ${this.content}")
        null
    }
    return a
}

fun VideoStream.toVideoSpec(): VideoSpec {
    val v = VideoSpec()
    v.isVideoOnly = this.isVideoOnly()
    v.bitrate = this.bitrate
    v.fps = this.fps
    v.width = this.width
    v.height = this.height
    v.quality = this.quality
    v.codec = this.codec
    v.deliveryMethod = this.deliveryMethod.name
    v.resolution = this.getResolution()
    v.url = if (this.isUrl) this.content else {
        Log.e("toVideoSpec", "VideoStream content is not url: ${this.content}")
        null
    }
    return v
}

internal fun String.toResolutionValue(): Int {
    val match = Regex("(\\d+)p|(\\d+)k").find(this)
    return when {
        match?.groupValues?.get(1) != null -> match.groupValues[1].toInt()
        match?.groupValues?.get(2) != null -> match.groupValues[2].toInt() * 1024
        else -> 0
    }
}

fun getSortedVStreams(videoStreams: List<VideoStream>?, videoOnlyStreams: List<VideoStream>?, ascendingOrder: Boolean, preferVideoOnlyStreams: Boolean): List<VideoSpec> {
    val videoStreamsOrdered = if (preferVideoOnlyStreams) listOf(videoStreams, videoOnlyStreams) else listOf(videoOnlyStreams, videoStreams)
    val allInitialStreams = videoStreamsOrdered.filterNotNull().flatten().toList()
    val comparator = compareBy<VideoStream> { it.getResolution().toResolutionValue() }
    val vList = mutableListOf<VideoSpec>()
    (if (ascendingOrder) allInitialStreams.sortedWith(comparator) else allInitialStreams.sortedWith(comparator.reversed())).forEach { vList.add(it.toVideoSpec()) }
    return vList
}
