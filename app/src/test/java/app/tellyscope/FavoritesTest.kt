// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope

import app.tellyscope.data.FakeDeviceSource
import app.tellyscope.data.NameBook
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FavoritesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun aStarSurvivesARestartAndFollowsTheTvToItsSerial() {
        val file = tmp.root.resolve("favorites.tsv")
        NameBook(file).set("192.168.1.64:5555", "1")
        val book = NameBook(file)
        assertEquals("1", book["192.168.1.64:5555"])
        book.rekey("192.168.1.64:5555", "sn:ABC")
        assertEquals("1", NameBook(file)["sn:ABC"])
        assertNull(NameBook(file)["192.168.1.64:5555"])
    }

    @Test fun unstarringRemovesIt() {
        val file = tmp.root.resolve("favorites.tsv")
        val book = NameBook(file)
        book.set("sn:ABC", "1")
        book.set("sn:ABC", null)
        assertNull(NameBook(file)["sn:ABC"])
    }

    @Test fun theFakeSourceStarsAndUnstars() = runTest {
        val source = FakeDeviceSource(FakeDeviceSource.Scenario.Normal)
        val id = source.known().first().id
        assertFalse(source.known().first { it.id == id }.favorite)
        source.setFavorite(id, true)
        assertTrue(source.known().first { it.id == id }.favorite)
        source.setFavorite(id, false)
        assertFalse(source.known().first { it.id == id }.favorite)
    }
}
