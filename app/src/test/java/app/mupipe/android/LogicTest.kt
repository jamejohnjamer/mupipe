package app.mupipe.android

import app.mupipe.android.data.model.SearchFilters
import app.mupipe.android.data.provider.SearchFilterMapper
import app.mupipe.android.util.DownloadStateMachine
import app.mupipe.android.util.HistoryPruner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test fun `30 day old history is kept, 31 day old is pruned`() {
        val now = 1_800_000_000_000L
        val day = 24L * 60 * 60 * 1000
        assertTrue(HistoryPruner.isWithinDays(now - 30 * day, 30, now))
        assertFalse(HistoryPruner.isWithinDays(now - 31 * day, 30, now))
    }

    @Test fun `paused download never auto-resumes on network back`() {
        assertNull(DownloadStateMachine.nextState("PausedByUser", DownloadStateMachine.Event.NetworkBack))
        assertEquals("Downloading", DownloadStateMachine.nextState("PausedByUser", DownloadStateMachine.Event.Resume))
    }

    @Test fun `failed download can retry, completed cannot move`() {
        assertEquals("Retrying", DownloadStateMachine.nextState("Failed", DownloadStateMachine.Event.Retry))
        assertNull(DownloadStateMachine.nextState("Completed", DownloadStateMachine.Event.Start))
    }

    @Test fun `max retries is 3`() {
        assertTrue(DownloadStateMachine.maxRetriesReached(3))
        assertFalse(DownloadStateMachine.maxRetriesReached(2))
    }

    @Test fun `filter mapping uses week not thisweek`() {
        val p = SearchFilterMapper.toParams(SearchFilters(date = "week", duration = "long"))
        assertEquals("week", p["date"])
        assertEquals("long", p["duration"])
    }

    @Test fun `unknown filter value throws`() {
        try {
            SearchFilterMapper.toParams(SearchFilters(date = "thisweek"))
            assertTrue("should have thrown", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("thisweek"))
        }
    }
}
