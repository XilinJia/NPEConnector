package ac.roma.npeconnector

import ac.mdiq.podcini.shared.AudioSpec
import ac.mdiq.podcini.shared.EpisodeIPC
import ac.mdiq.podcini.shared.VideoSpec
import android.util.Log
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream

fun StreamInfoItem.toEpisodeIPC(isAudio: Boolean = false): EpisodeIPC {
    val e = EpisodeIPC()
    e.link = this.url
    e.title = this.name
    e.description = "Short: ${this.shortDescription}"
    e.imageUrl = this.thumbnails.first().url
    e.pubDate = this.uploadDate?.localDateTime?.toKotlinLocalDateTime()?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds() ?: 0
    e.viewCount = this.viewCount.toInt()
    e.size = 0
    e.mimeType = if (isAudio) "audio/*" else "video/*"
    e.fileUrl = null
    e.downloadUrl = this.url
    if (this.duration > 0) e.duration = this.duration.toInt() * 1000
    //            e.likeCount = this.likeCount.toInt() // TODO: need to get likeCount
    return e
}

fun StreamInfo.toEpisodeIPC(isAudio: Boolean = false): EpisodeIPC {
    val e = EpisodeIPC()
    e.link = this.url
    e.title = this.name
    e.description = this.description?.content
    e.imageUrl = this.thumbnails.first().url
    e.pubDate = this.uploadDate?.localDateTime?.let { LocalDateTime(year = it.year, month = it.monthValue, day = it.dayOfMonth, hour = it.hour, minute = it.minute, second = it.second).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() } ?: 0
    e.viewCount = this.viewCount.toInt()
    e.likeCount = this.likeCount.toInt()
    e.downloadUrl = this.url
    e.size = 0
    e.mimeType = if (isAudio) "audio/*" else "video/*"
    if (this.duration > 0) e.duration = this.duration.toInt() * 1000
    return e
}

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
