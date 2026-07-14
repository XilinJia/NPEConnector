package ac.roma.npeconnector

import ac.mdiq.podcini.sources.IFeedSearchProvider
import ac.mdiq.podcini.shared.FeedSearchResult
import android.util.Log
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo

open class FeedSearcher(val name_: String, val serviceId: Int, val contentFilter: List<String> = listOf("channels", "playlists")) : IFeedSearchProvider.Stub() {
    fun ChannelInfoItem.toFeedSearchResult(): FeedSearchResult {
        val title = this.name
        val imageUrl: String? = if (this.thumbnails.isNotEmpty()) this.thumbnails[0].url else null
        val feedUrl = this.url
        val author = ""
        val count: Int = this.streamCount.toInt()
        val update: String? = null
        val subscriberCount = this.subscriberCount.toInt()
        return FeedSearchResult(title, imageUrl, feedUrl, author, count, update, subscriberCount, name_)
    }

    fun PlaylistInfoItem.toFeedSearchResult(): FeedSearchResult {
        val title = this.name
        val imageUrl: String? = if (this.thumbnails.isNotEmpty()) this.thumbnails[0].url else null
        val feedUrl = this.url
        val author = ""
        val count: Int = this.streamCount.toInt()
        val update: String? = null
        val subscriberCount = 0
        return FeedSearchResult(title, imageUrl, feedUrl, author, count, update, subscriberCount, name_)
    }

    override fun search(query: String): List<FeedSearchResult> {
        val service = try { NewPipe.getService(serviceId) } catch (e: ExtractionException) { throw ExtractionException("NewPipe service not found") }
        try {
            val podResults: MutableList<FeedSearchResult> = mutableListOf()
            for (filer in contentFilter) {
                val searchInfo = SearchInfo.getInfo(service, service.searchQHFactory.fromQuery(query, listOf(filer), ""))
                for (item in searchInfo.relatedItems) {
                    when (item) {
                        is ChannelInfoItem -> podResults.add(item.toFeedSearchResult())
                        is PlaylistInfoItem -> podResults.add(item.toFeedSearchResult())
                    }
                }
            }
            return podResults
        } catch (e: Throwable) { Log.e("FeedSearcher", "error: ${e.message}") }
        return listOf()
    }

    override fun lookupUrl(url: String): String = url

    override fun urlNeedsLookup(url: String): Boolean = false
    override fun getName(): String = name_
}
