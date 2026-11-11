package app.mupipe.android

import app.mupipe.android.util.ImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportParserTest {
    @Test fun `bare channel id is recognised`() {
        val e = ImportParser.parseImportEntries("UC_x5XG1OV2P6uZZ5FSM9Ttw").first()
        assertTrue(e is ImportParser.Entry.ChannelId)
    }

    @Test fun `handle and comma split work`() {
        val list = ImportParser.parseImportEntries("@google, UC_x5XG1OV2P6uZZ5FSM9Ttw")
        assertEquals(2, list.size)
        assertTrue(list[0] is ImportParser.Entry.Handle)
    }

    @Test fun `channel url gives channel id`() {
        val e = ImportParser.parseImportEntries("https://www.youtube.com/channel/UC_x5XG1OV2P6uZZ5FSM9Ttw").first()
        assertTrue(e is ImportParser.Entry.ChannelId || e is ImportParser.Entry.ChannelUrl)
    }

    @Test fun `unknown stays unknown`() {
        val e = ImportParser.parseImportEntries("not a channel at all").first()
        assertTrue(e is ImportParser.Entry.Unknown)
    }

    @Test fun `duplicates are case-insensitive`() {
        val entries = ImportParser.parseImportEntries("UC_x5XG1OV2P6uZZ5FSM9Ttw")
        val (dups, fresh) = ImportParser.findDuplicates(entries, listOf("uc_x5xg1ov2p6uzz5fsm9ttw"))
        assertEquals(1, dups.size)
        assertEquals(0, fresh.size)
    }

    @Test fun `empty input gives empty list`() {
        assertEquals(0, ImportParser.parseImportEntries("").size)
        assertEquals(0, ImportParser.parseImportEntries(null).size)
    }
}
