package com.zerowasteeats.app.data.session

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Keeps the backup rules in step with the file [PrefsSessionStore] really writes. */
class BackupRulesTest {

    private val sessionFile = "${PrefsSessionStore.FILE_NAME}.xml"

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
    fun fullBackupContent_excludesTheSessionFile() {
        val rules = root("backup_rules.xml")

        assertEquals(listOf(sessionFile), sharedPrefExcludes(rules))
    }

    @Test
    fun cloudBackup_excludesTheSessionFile() {
        val rules = root("data_extraction_rules.xml")

        assertEquals(listOf(sessionFile), sharedPrefExcludes(child(rules, "cloud-backup")))
    }

    @Test
    fun deviceTransfer_excludesTheSessionFile() {
        val rules = root("data_extraction_rules.xml")

        assertEquals(listOf(sessionFile), sharedPrefExcludes(child(rules, "device-transfer")))
    }

    @Test
    fun rules_doNotRestrictAnythingElse() {
        // No <include>: the theme preference and the like keep being backed up
        listOf(root("backup_rules.xml"), root("data_extraction_rules.xml")).forEach { rules ->
            assertEquals(0, rules.getElementsByTagName("include").length)
        }
    }
}
