package com.zerowasteeats.app.data.session

import com.zerowasteeats.app.data.auth.PrefsTokenStore
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/**
 * Keeps the backup rules in step with the files [PrefsSessionStore] and [PrefsTokenStore]
 * really write: neither who is signed in nor their tokens may travel to another device.
 */
class BackupRulesTest {

    private val sessionFile = "${PrefsSessionStore.FILE_NAME}.xml"
    private val tokenFile = "${PrefsTokenStore.FILE_NAME}.xml"
    private val excludedFiles = listOf(sessionFile, tokenFile)

    // Gradle runs unit tests from the module folder
    private fun root(name: String): Element {
        val file = File("src/main/res/xml/$name")
        assertTrue("missing ${file.absolutePath}", file.exists())
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(file).documentElement
    }

    private fun sharedPrefExcludes(parent: Element): List<String> =
        (0 until parent.childNodes.length)
            .map { parent.childNodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == "exclude" && it.getAttribute("domain") == "sharedpref" }
            .map { it.getAttribute("path") }

    private fun child(parent: Element, tag: String): Element =
        parent.getElementsByTagName(tag).item(0) as Element

    @Test
    fun fullBackupContent_excludesTheSessionAndTokenFiles() {
        val rules = root("backup_rules.xml")

        assertEquals(excludedFiles, sharedPrefExcludes(rules))
    }

    @Test
    fun cloudBackup_excludesTheSessionAndTokenFiles() {
        val rules = root("data_extraction_rules.xml")

        assertEquals(excludedFiles, sharedPrefExcludes(child(rules, "cloud-backup")))
    }

    @Test
    fun deviceTransfer_excludesTheSessionAndTokenFiles() {
        val rules = root("data_extraction_rules.xml")

        assertEquals(excludedFiles, sharedPrefExcludes(child(rules, "device-transfer")))
    }

    @Test
    fun tokensLiveInTheirOwnFile() {
        // Keeping them apart lets the session be read at startup without touching credentials
        assertTrue(sessionFile != tokenFile)
    }

    @Test
    fun rules_doNotRestrictAnythingElse() {
        // No <include>: the theme preference and the like keep being backed up
        listOf(root("backup_rules.xml"), root("data_extraction_rules.xml")).forEach { rules ->
            assertEquals(0, rules.getElementsByTagName("include").length)
        }
    }
}
