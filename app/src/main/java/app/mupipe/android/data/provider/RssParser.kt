package app.mupipe.android.data.provider

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/** One entry from youtube.com/feeds/videos.xml — latest 15 only, no key. */
data class RssEntry(val videoId: String, val title: String, val publishedMillis: Long?)

/** Minimal pull parser. Runs on IO dispatcher only, never on main thread. */
object RssParser {
    fun parse(xml: String): List<RssEntry> {
        val out = mutableListOf<RssEntry>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))
        var videoId: String? = null
        var title: String? = null
        var published: Long? = null
        var inEntry = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> { inEntry = true; videoId = null; title = null; published = null }
                    "videoId" -> if (inEntry) videoId = parser.nextText()
                    "title" -> if (inEntry) title = parser.nextText()
                    "published" -> if (inEntry) {
                        published = runCatching {
                            java.time.Instant.parse(parser.nextText()).toEpochMilli()
                        }.getOrNull()
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "entry" && inEntry) {
                    inEntry = false
                    if (videoId != null && title != null) {
                        out.add(RssEntry(videoId, title, published))
                    }
                }
            }
            event = parser.next()
        }
        return out
    }

    fun feedUrlForChannel(channelId: String): String =
        "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
}
