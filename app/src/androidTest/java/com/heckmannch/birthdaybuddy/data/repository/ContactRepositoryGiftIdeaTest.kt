package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.util.WidgetUpdater
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Instrumentierte Tests für die Gift-Idea-Operationen in [ContactRepository].
 *
 * Strategie: Beide Room-Datenbanken werden als In-Memory-Instanzen erstellt.
 * Nicht-DB-Abhängigkeiten (SystemContactDataSource, CalendarSyncRepository,
 * GiftIdeaBackupManager, WidgetUpdater) werden als leere Mocks bereitgestellt,
 * da sie in den zu testenden Codepfaden nicht aufgerufen werden.
 *
 * Verifiziertes Muster: Alle vier Gift-Idea-Operationen delegieren an die
 * private Hilfsmethode updateGiftIdeas(), die das Best-Effort-Rollback-Muster
 * implementiert:
 *  1. SettingsDatabase (Quelle der Wahrheit) atomar via withTransaction {} schreiben.
 *  2. AppDatabase-Cache in try/catch mit Rollback auf den vorherigen Stand absichern.
 */
@RunWith(AndroidJUnit4::class)
class ContactRepositoryGiftIdeaTest {

    private lateinit var appDb: AppDatabase
    private lateinit var settingsDb: SettingsDatabase
    private lateinit var repository: ContactRepository

    private fun makeContact(lookupKey: String, name: String) = Contact(
        contactId = lookupKey,
        lookupKey = lookupKey,
        fullName = name,
    )

    private fun makeIdea(text: String, isChecked: Boolean = false) =
        GiftIdea(text = text, isChecked = isChecked)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        appDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        settingsDb = Room.inMemoryDatabaseBuilder(context, SettingsDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = ContactRepository(
            context = context,
            contactDao = appDb.contactDao(),
            labelConfigDao = settingsDb.labelConfigDao(),
            appSettingsDao = settingsDb.appSettingsDao(),
            contactUserDataDao = settingsDb.contactUserDataDao(),
            systemContactDataSource = mock(),
            giftIdeaBackupManager = mock(),
            calendarSyncRepository = mock(),
            widgetUpdater = mock<WidgetUpdater>(),
            appDatabase = appDb,
            settingsDatabase = settingsDb,
        )
    }

    @After
    fun tearDown() {
        appDb.close()
        settingsDb.close()
    }

    // ─────────────────────────────────
    // addGiftIdea Tests
    // ─────────────────────────────────

    @Test
    fun addGiftIdea_addsIdea_inBothDatabases() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        val idea = makeIdea("Buch über Kotlin")

        // Act
        repository.addGiftIdea("alice", idea)

        // Assert – AppDB Cache
        val cachedContact = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(cachedContact?.giftIdeas).hasSize(1)
        assertThat(cachedContact?.giftIdeas?.first()?.text).isEqualTo("Buch über Kotlin")

        // Assert – SettingsDB (Quelle der Wahrheit)
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas).hasSize(1)
        assertThat(userData?.giftIdeas?.first()?.text).isEqualTo("Buch über Kotlin")
    }

    @Test
    fun addGiftIdea_preservesExistingIdeas() = runTest {
        // Arrange – Alice hat bereits eine Idee in der DB
        val existingIdea = makeIdea("Blumen")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(existingIdea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(existingIdea))
        )
        val newIdea = makeIdea("Schokolade")

        // Act
        repository.addGiftIdea("alice", newIdea)

        // Assert – beide Ideen vorhanden, bestehende nicht überschrieben
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas).hasSize(2)
        val texts = userData?.giftIdeas?.map { it.text }
        assertThat(texts).contains("Blumen")
        assertThat(texts).contains("Schokolade")
    }

    @Test
    fun addGiftIdea_insertsNewIdeaBeforeCheckedItems() = runTest {
        // Arrange – Eine bereits erledigte Idee vorhanden
        val doneIdea = makeIdea("Erledigt", isChecked = true)
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(doneIdea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(doneIdea))
        )
        val newIdea = makeIdea("Neu")

        // Act
        repository.addGiftIdea("alice", newIdea)

        // Assert – neue Idee steht vor der erledigten
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas).hasSize(2)
        assertThat(userData?.giftIdeas?.first()?.text).isEqualTo("Neu")
        assertThat(userData?.giftIdeas?.last()?.isChecked).isTrue()
    }

    @Test
    fun addGiftIdea_doesNothing_whenContactNotInDb() = runTest {
        // Act – Kontakt existiert nicht, keine Exception erwartet
        repository.addGiftIdea("nonexistent_key", makeIdea("Idee"))

        // Assert – keine Daten angelegt
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("nonexistent_key")
        assertThat(userData).isNull()
    }

    // ─────────────────────────────────
    // toggleGiftIdea Tests
    // ─────────────────────────────────

    @Test
    fun toggleGiftIdea_setsIsChecked_inBothDatabases() = runTest {
        // Arrange
        val idea = makeIdea("Buch")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act – Idee als erledigt markieren
        repository.toggleGiftIdea("alice", idea, isChecked = true)

        // Assert – AppDB Cache
        val cachedContact = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(cachedContact?.giftIdeas?.first()?.isChecked).isTrue()

        // Assert – SettingsDB (Quelle der Wahrheit)
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas?.first()?.isChecked).isTrue()
    }

    @Test
    fun toggleGiftIdea_movesCheckedItemToEnd() = runTest {
        // Arrange – zwei offene Ideen
        val idea1 = makeIdea("Idee A")
        val idea2 = makeIdea("Idee B")
        val ideas = listOf(idea1, idea2)
        appDb.contactDao().upsertContact(makeContact("alice", "Alice").copy(giftIdeas = ideas))
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = ideas)
        )

        // Act – erste Idee abhaken
        repository.toggleGiftIdea("alice", idea1, isChecked = true)

        // Assert – idea1 ist jetzt am Ende (erledigt)
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas).hasSize(2)
        assertThat(userData?.giftIdeas?.last()?.id).isEqualTo(idea1.id)
        assertThat(userData?.giftIdeas?.last()?.isChecked).isTrue()
        assertThat(userData?.giftIdeas?.first()?.id).isEqualTo(idea2.id)
    }

    @Test
    fun toggleGiftIdea_setsIsUnchecked_inBothDatabases() = runTest {
        // Arrange – Idee ist bereits erledigt
        val idea = makeIdea("Buch", isChecked = true)
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act – Idee wieder öffnen
        repository.toggleGiftIdea("alice", idea, isChecked = false)

        // Assert – nicht mehr erledigt in beiden DBs
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas?.first()?.isChecked).isFalse()

        val cachedContact = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(cachedContact?.giftIdeas?.first()?.isChecked).isFalse()
    }

    // ─────────────────────────────────
    // deleteGiftIdea Tests
    // ─────────────────────────────────

    @Test
    fun deleteGiftIdea_removesIdea_fromBothDatabases() = runTest {
        // Arrange
        val idea = makeIdea("Zu löschen")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act
        repository.deleteGiftIdea("alice", idea.id)

        // Assert – AppDB Cache leer
        val cachedContact = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(cachedContact?.giftIdeas).isEmpty()

        // Assert – SettingsDB (Quelle der Wahrheit) leer
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas).isEmpty()
    }

    @Test
    fun deleteGiftIdea_preservesOtherIdeas() = runTest {
        // Arrange – zwei Ideen, nur eine soll gelöscht werden
        val ideaToDelete = makeIdea("Löschen")
        val ideaToKeep = makeIdea("Behalten")
        val ideas = listOf(ideaToDelete, ideaToKeep)
        appDb.contactDao().upsertContact(makeContact("alice", "Alice").copy(giftIdeas = ideas))
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = ideas)
        )

        // Act
        repository.deleteGiftIdea("alice", ideaToDelete.id)

        // Assert – nur ideaToKeep übrig
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas).hasSize(1)
        assertThat(userData?.giftIdeas?.first()?.text).isEqualTo("Behalten")
    }

    @Test
    fun deleteGiftIdea_doesNothing_whenIdeaIdNotFound() = runTest {
        // Arrange
        val idea = makeIdea("Existierende Idee")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act – mit unbekannter ID löschen
        repository.deleteGiftIdea("alice", "nonexistent_idea_id")

        // Assert – ursprüngliche Idee unverändert vorhanden
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas).hasSize(1)
        assertThat(userData?.giftIdeas?.first()?.text).isEqualTo("Existierende Idee")
    }

    // ─────────────────────────────────
    // updateGiftIdeaText Tests
    // ─────────────────────────────────

    @Test
    fun updateGiftIdeaText_updatesText_inBothDatabases() = runTest {
        // Arrange
        val idea = makeIdea("Alter Text")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act
        repository.updateGiftIdeaText("alice", idea.id, "Neuer Text")

        // Assert – AppDB Cache
        val cachedContact = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(cachedContact?.giftIdeas?.first()?.text).isEqualTo("Neuer Text")

        // Assert – SettingsDB (Quelle der Wahrheit)
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas?.first()?.text).isEqualTo("Neuer Text")
    }

    @Test
    fun updateGiftIdeaText_preservesIdeaId_andCheckedState() = runTest {
        // Arrange – erledigte Idee
        val idea = makeIdea("Original", isChecked = true)
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act
        repository.updateGiftIdeaText("alice", idea.id, "Geändert")

        // Assert – ID und isChecked unverändert, nur Text geändert
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val updated = userData?.giftIdeas?.first()
        assertThat(updated?.id).isEqualTo(idea.id)
        assertThat(updated?.isChecked).isTrue()
        assertThat(updated?.text).isEqualTo("Geändert")
    }

    @Test
    fun updateGiftIdeaText_doesNothing_whenIdeaIdNotFound() = runTest {
        // Arrange
        val idea = makeIdea("Original")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act – unbekannte Ideen-ID
        repository.updateGiftIdeaText("alice", "nonexistent_idea_id", "Neuer Text")

        // Assert – Original unverändert
        val userData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(userData?.giftIdeas?.first()?.text).isEqualTo("Original")
    }

    // ─────────────────────────────────
    // Datenbank-Konsistenz: SettingsDB ↔ AppDB
    // ─────────────────────────────────

    @Test
    fun settingsDb_andAppDb_areConsistentAfterAdd() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        val idea = makeIdea("Konsistenz-Test")

        // Act
        repository.addGiftIdea("alice", idea)

        // Assert – beide DBs zeigen identische Gift-Idea-Listen
        val settingsIdeas = settingsDb.contactUserDataDao()
            .getUserDataForContact("alice")?.giftIdeas
        val cacheIdeas = appDb.contactDao()
            .getContactByLookupKey("alice")?.giftIdeas

        assertThat(settingsIdeas?.map { it.text })
            .isEqualTo(cacheIdeas?.map { it.text })
        assertThat(settingsIdeas?.map { it.isChecked })
            .isEqualTo(cacheIdeas?.map { it.isChecked })
    }

    @Test
    fun settingsDb_andAppDb_areConsistentAfterDelete() = runTest {
        // Arrange – zwei Ideen anlegen
        val idea1 = makeIdea("Erste")
        val idea2 = makeIdea("Zweite")
        val ideas = listOf(idea1, idea2)
        appDb.contactDao().upsertContact(makeContact("alice", "Alice").copy(giftIdeas = ideas))
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = ideas)
        )

        // Act – eine löschen
        repository.deleteGiftIdea("alice", idea1.id)

        // Assert – beide DBs zeigen nur noch idea2
        val settingsIdeas = settingsDb.contactUserDataDao()
            .getUserDataForContact("alice")?.giftIdeas
        val cacheIdeas = appDb.contactDao()
            .getContactByLookupKey("alice")?.giftIdeas

        assertThat(settingsIdeas).hasSize(1)
        assertThat(cacheIdeas).hasSize(1)
        assertThat(settingsIdeas?.first()?.text).isEqualTo(cacheIdeas?.first()?.text)
    }

    @Test
    fun settingsDb_andAppDb_areConsistentAfterToggle() = runTest {
        // Arrange
        val idea = makeIdea("Toggle-Test")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act
        repository.toggleGiftIdea("alice", idea, isChecked = true)

        // Assert – beide DBs zeigen identischen isChecked-Zustand
        val settingsChecked = settingsDb.contactUserDataDao()
            .getUserDataForContact("alice")?.giftIdeas?.first()?.isChecked
        val cacheChecked = appDb.contactDao()
            .getContactByLookupKey("alice")?.giftIdeas?.first()?.isChecked

        assertThat(settingsChecked).isEqualTo(cacheChecked)
        assertThat(settingsChecked).isTrue()
    }

    @Test
    fun settingsDb_andAppDb_areConsistentAfterTextUpdate() = runTest {
        // Arrange
        val idea = makeIdea("Alt")
        appDb.contactDao().upsertContact(
            makeContact("alice", "Alice").copy(giftIdeas = listOf(idea))
        )
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = listOf(idea))
        )

        // Act
        repository.updateGiftIdeaText("alice", idea.id, "Neu")

        // Assert – beide DBs zeigen denselben Text
        val settingsText = settingsDb.contactUserDataDao()
            .getUserDataForContact("alice")?.giftIdeas?.first()?.text
        val cacheText = appDb.contactDao()
            .getContactByLookupKey("alice")?.giftIdeas?.first()?.text

        assertThat(settingsText).isEqualTo("Neu")
        assertThat(cacheText).isEqualTo("Neu")
        assertThat(settingsText).isEqualTo(cacheText)
    }

    // ─────────────────────────────────
    // Lifecycle: Add → Toggle → Delete
    // ─────────────────────────────────

    @Test
    fun fullLifecycle_add_toggle_delete_resultsInConsistentFinalState() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        val idea = makeIdea("Lifecycle-Idee")

        // Act – vollständiger Lebenszyklus
        repository.addGiftIdea("alice", idea)
        repository.toggleGiftIdea("alice", idea, isChecked = true)
        repository.deleteGiftIdea("alice", idea.id)

        // Assert – beide DBs leer
        val settingsIdeas = settingsDb.contactUserDataDao()
            .getUserDataForContact("alice")?.giftIdeas
        val cacheIdeas = appDb.contactDao()
            .getContactByLookupKey("alice")?.giftIdeas

        assertThat(settingsIdeas).isEmpty()
        assertThat(cacheIdeas).isEmpty()
    }
}
