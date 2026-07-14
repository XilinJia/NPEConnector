package ac.roma.npeconnector

import ac.mdiq.podcini.shared.EpisodeIPC
import ac.mdiq.podcini.sources.IMediaSearchProvider
import android.util.Log
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

open class MediaSearcher(val name_: String, val serviceId: Int, val contentFilter: List<String> = listOf("videos")) : IMediaSearchProvider.Stub() {
    val service: StreamingService by lazy { try { NewPipe.getService(serviceId) } catch (e: ExtractionException) { throw ExtractionException("NewPipe service not found") } }
    var page: Page? = null
    var handler: SearchQueryHandler? = null

    override fun search(query: String, limit: Int): List<EpisodeIPC> {
        try {
            val podResults: MutableList<EpisodeIPC> = mutableListOf()
            handler = service.searchQHFactory.fromQuery(query, contentFilter, "")
            val searchInfo = SearchInfo.getInfo(service, handler)
            var items = searchInfo.relatedItems
            page = searchInfo.nextPage
            while (items.isNotEmpty()) {
                for (m in items) if (m is StreamInfoItem) podResults.add(m.toEpisodeIPC(isAudio = serviceId==1))
                if (podResults.size >= limit) break
                if (!searchInfo.hasNextPage()) break
                val itemsPage = SearchInfo.getMoreItems(service, handler, page)
                page = itemsPage.nextPage
                items = itemsPage.items
            }
            return podResults
        } catch (e: Throwable) { Log.e("MediaSearcher", "error: ${e.message}") }
        return listOf()
    }

    override fun searchQuick(query: String): List<EpisodeIPC> {
        try {
            val podResults: MutableList<EpisodeIPC> = mutableListOf()
            handler = service.searchQHFactory.fromQuery(query, contentFilter, "")
            val searchInfo = SearchInfo.getInfo(service, handler)
            val items = searchInfo.relatedItems
            page = searchInfo.nextPage
            for (m in items) if (m is StreamInfoItem) podResults.add(m.toEpisodeIPC(isAudio = serviceId==1))
            return podResults
        } catch (e: Throwable) { Log.e("MediaSearcher", "error: ${e.message}") }
        return listOf()
    }

    override fun getMoreItems(): List<EpisodeIPC> {
        try {
            val itemsPage = SearchInfo.getMoreItems(service, handler, page)
            page = itemsPage.nextPage
            val items = itemsPage.items
            val podResults: MutableList<EpisodeIPC> = mutableListOf()
            for (m in items) if (m is StreamInfoItem) podResults.add(m.toEpisodeIPC(isAudio = serviceId==1))
            return podResults
        } catch (e: Throwable) { Log.e("MediaSearcher", "error: ${e.message}") }
        return listOf()
    }

    override fun getName(): String = name_
}
