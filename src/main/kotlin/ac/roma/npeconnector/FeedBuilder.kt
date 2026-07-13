package ac.roma.npeconnector

import ac.mdiq.podcini.shared.EpisodeIPC
import ac.mdiq.podcini.shared.FeedIPC
import ac.mdiq.podcini.shared.getEntityId
import ac.mdiq.podcini.shared.prepareUrl
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class FeedBuilder(val feedType: String, var urlInit: String, val service: StreamingService) {
    private val TAG = "FeedBuilder"

    var selectedDownloadUrl: String? = null

    var channelInfo: ChannelInfo? = null

    private var playlistInfo: PlaylistInfo? = null

    private var feedId: Long = 0L

    private var nextPage: Page? = null

    private var curItemIndex: Int = 0

    private var streamInfoItems: List<StreamInfoItem> = listOf()

    private var infoItems: List<InfoItem> = listOf()

    private fun setupFeed(): FeedIPC {
        val feed_ = FeedIPC()
        feed_.downloadUrl = selectedDownloadUrl
        feedId = getEntityId()
        feed_.id = feedId
        feed_.type = feedType
        feed_.hasVideoMedia = true
        feed_.prefStreamOverDownload = true
        feed_.episodesDownloadable = false
        feed_.autoDownload = false
        return feed_
    }

    suspend fun feedFromChannel(index: Int, title: String): FeedIPC? {
        val cInfo = channelInfo ?: return null
        if (index >= cInfo.tabs.size) return null
        var url = cInfo.tabs[index].url
        if (!url.startsWith("http")) url = urlInit
        return try {
            selectedDownloadUrl = prepareUrl(url)
            val channelTabInfo = ChannelTabInfo.getInfo(service, cInfo.tabs[index])
            val feed_ = setupFeed()
            feed_.title = cInfo.name + " " + title
            feed_.description = cInfo.description
            feed_.author = cInfo.parentChannelName
            feed_.imageUrl = if (cInfo.avatars.isNotEmpty()) cInfo.avatars.first().url else null
            infoItems = channelTabInfo.relatedItems
            nextPage = channelTabInfo.nextPage
            withContext(Dispatchers.Main) { return@withContext feed_ }
        } catch (e: Throwable) {
            Log.e(TAG, "feedFromChannel error ${e.message}")
            withContext(Dispatchers.Main) { return@withContext null }
        }
    }

    suspend fun episodesFromChannel(total: Int, since: Long = 0L): List<EpisodeIPC> {
        val cInfo = channelInfo ?: return listOf()

        val titleSet = hashSetOf<String>()
        var count = 0
        val eList = mutableSetOf<EpisodeIPC>()
        while (infoItems.isNotEmpty()) {
            for (i in curItemIndex until infoItems.size) {
                val r = infoItems[i] as? StreamInfoItem ?: continue
                count++
                curItemIndex = i + 1
                if (r.infoType != InfoItem.InfoType.STREAM) continue
                if ((r.uploadDate?.localDateTime?.toKotlinLocalDateTime()?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds() ?: 0) <= since) {
                    nextPage = null
                    break
                }
                val e = r.toEpisodeIPC()
                if (e.title == null || e.title in titleSet) continue
                titleSet.add(e.title!!)
                e.feedId = feedId
                eList.add(e)
                if (total > 0 && eList.size >= total) return eList.toList()
            }
            if (nextPage == null || count > 2 * EPISODES_LIMIT || eList.size > EPISODES_LIMIT) return eList.toList()
            try {
                val page = ChannelTabInfo.getMoreItems(service, cInfo.tabs.first(), nextPage!!)
                nextPage = page?.nextPage
                infoItems = page?.items ?: listOf()
                curItemIndex = 0
            } catch (e: Throwable) {
                Log.e(TAG, "episodesFromChannel error ${e.message}")
                withContext(Dispatchers.Main) { return@withContext null }
                break
            }
        }
        return eList.toList()
    }

    suspend fun feedFromPlaylist(): FeedIPC? {
        return try {
            playlistInfo = PlaylistInfo.getInfo(service, urlInit) ?: return null
            selectedDownloadUrl = prepareUrl(urlInit)
            val feed_ = setupFeed()
            feed_.title = playlistInfo!!.name
            feed_.description = playlistInfo?.description?.content ?: ""
            feed_.author = playlistInfo?.uploaderName
            feed_.imageUrl = if (playlistInfo!!.thumbnails.isNotEmpty()) playlistInfo!!.thumbnails.first().url else null
            streamInfoItems = playlistInfo!!.relatedItems
            nextPage = playlistInfo?.nextPage
            withContext(Dispatchers.Main) { return@withContext feed_ }
        } catch (e: Throwable) {
            Log.e(TAG, "feedFromPlaylist error ${e.message}")
            withContext(Dispatchers.Main) { return@withContext null }
        }
    }

    suspend fun episodesFromList(total: Int, since: Long = 0L): List<EpisodeIPC> {
        val titleSet = hashSetOf<String>()
        var count = 0
        val eList = mutableSetOf<EpisodeIPC>()
        while (streamInfoItems.isNotEmpty()) {
            for (i in curItemIndex until streamInfoItems.size) {
                val r = streamInfoItems[i]
                curItemIndex = i + 1
                if (r.infoType != InfoItem.InfoType.STREAM) {
//                    Log.d(TAG, "episodesFromList relatedItem is not STREAM, ignored")
                    continue
                }
                if ((r.uploadDate?.localDateTime?.toKotlinLocalDateTime()?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds() ?: 0) <= since) {
                    nextPage = null
                    break
                }
                count++
                val e = r.toEpisodeIPC()
                if (e.title == null || e.title in titleSet) continue
                e.feedId = feedId
                eList.add(e)
                if (total > 0 && eList.size >= total) return eList.toList()
            }
            if (nextPage == null || count > EPISODES_LIMIT) return eList.toList()
            try {
                val page = PlaylistInfo.getMoreItems(service, urlInit, nextPage)
                nextPage = page?.nextPage
                streamInfoItems = page?.items ?: listOf()
                curItemIndex = 0
            } catch (e: Throwable) {
                Log.e(TAG, "episodesFromList PlaylistInfo.getMoreItems error: ${e.message}")
                withContext(Dispatchers.Main) { return@withContext null }
                break
            }
        }
        return eList.toList()
    }

    companion object {
        const val EPISODES_LIMIT = 5000
    }
}
