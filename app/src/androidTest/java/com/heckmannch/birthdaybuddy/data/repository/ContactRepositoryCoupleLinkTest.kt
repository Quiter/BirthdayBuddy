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
import com.heckmannch.birthdaybuddy.util.WidgetUpdater
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Instrumentierte Tests für die Couple-Operationen in [ContactRepository].
 *
 * Strategie: Beide Room-Datenbanken werden als In-Memory-Instanzen erstellt.
 * Nicht-DB-Abhängigkeiten (SystemContactDataSource, CalendarSyncRepository,
 * GiftIdeaBackupManager, WidgetUpdater) werden als leere Mocks bereitgestellt,
 * da sie in den zu testenden Codepfaden nicht aufgerufen werden.
 */
@RunWith(AndroidJUnit4::class)
class ContactRepositoryCoupleLinkTest {

    private lateinit var appDb: AppDatabase
    private lateinit var settingsDb: SettingsDatabase
    private lateinit var repository: ContactRepository

    private fun makeContact(lookupKey: String, name: String) = Contact(
        contactId = lookupKey,
        lookupKey = lookupKey,
        fullName = name
    )

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

    // ─────────────────────────────────────────────────
    // linkAsCouple Tests
    // ─────────────────────────────────────────────────

    @Test
    fun linkAsCouple_setsSpouseLookupKey_inBothDatabases() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))

        // Act
        repository.linkAsCouple("alice", "bob")

        // Assert – AppDB Cache
        val alice = appDb.contactDao().getContactByLookupKey("alice")
        val bob = appDb.contactDao().getContactByLookupKey("bob")
        assertThat(alice?.spouseLookupKey).isEqualTo("bob")
        assertThat(bob?.spouseLookupKey).isEqualTo("alice")

        // Assert – SettingsDB (Quelle der Wahrheit)
        val aliceData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val bobData = settingsDb.contactUserDataDao().getUserDataForContact("bob")
        assertThat(aliceData?.spouseLookupKey).isEqualTo("bob")
        assertThat(bobData?.spouseLookupKey).isEqualTo("alice")
    }

    @Test
    fun linkAsCouple_preservesExistingGiftIdeas() = runTest {
        // Arrange – Alice hat bereits ContactUserData mit leerem Gift-Ideas-Stand
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))
        settingsDb.contactUserDataDao().upsertUserData(
            ContactUserData(lookupKey = "alice", giftIdeas = emptyList(), spouseLookupKey = null)
        )

        // Act
        repository.linkAsCouple("alice", "bob")

        // Assert – Gift Ideas nicht überschrieben
        val aliceData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        assertThat(aliceData?.giftIdeas).isEmpty()
        assertThat(aliceData?.spouseLookupKey).isEqualTo("bob")
    }

    @Test
    fun linkAsCouple_idempotent_whenCalledTwiceWithSameKeys() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))

        // Act – zweimal aufrufen
        repository.linkAsCouple("alice", "bob")
        repository.linkAsCouple("alice", "bob")

        // Assert – Daten konsistent, kein doppelter/fehlerhafter Zustand
        val aliceData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val bobData = settingsDb.contactUserDataDao().getUserDataForContact("bob")
        assertThat(aliceData?.spouseLookupKey).isEqualTo("bob")
        assertThat(bobData?.spouseLookupKey).isEqualTo("alice")
    }

    @Test
    fun linkAsCouple_worksWhenNoContactUserDataExistsPreviously() = runTest {
        // Arrange – keine ContactUserData vorhanden
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))

        // Act
        repository.linkAsCouple("alice", "bob")

        // Assert – ContactUserData wurde korrekt angelegt
        val aliceData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val bobData = settingsDb.contactUserDataDao().getUserDataForContact("bob")
        assertThat(aliceData).isNotNull()
        assertThat(bobData).isNotNull()
        assertThat(aliceData?.spouseLookupKey).isEqualTo("bob")
        assertThat(bobData?.spouseLookupKey).isEqualTo("alice")
    }

    // ─────────────────────────────────────────────────
    // unlinkCouple Tests
    // ─────────────────────────────────────────────────

    @Test
    fun unlinkCouple_clearsSpouseLookupKey_inBothDatabases() = runTest {
        // Arrange – erst verknüpfen
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))
        repository.linkAsCouple("alice", "bob")

        // Act
        repository.unlinkCouple("alice")

        // Assert – AppDB Cache
        val alice = appDb.contactDao().getContactByLookupKey("alice")
        val bob = appDb.contactDao().getContactByLookupKey("bob")
        assertThat(alice?.spouseLookupKey).isNull()
        assertThat(bob?.spouseLookupKey).isNull()

        // Assert – SettingsDB
        val aliceData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val bobData = settingsDb.contactUserDataDao().getUserDataForContact("bob")
        assertThat(aliceData?.spouseLookupKey).isNull()
        assertThat(bobData?.spouseLookupKey).isNull()
    }

    @Test
    fun unlinkCouple_canBeCalledFromEitherPartner() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))
        repository.linkAsCouple("alice", "bob")

        // Act – vom Partner (bob) aus trennen
        repository.unlinkCouple("bob")

        // Assert – beide getrennt
        val alice = appDb.contactDao().getContactByLookupKey("alice")
        val bob = appDb.contactDao().getContactByLookupKey("bob")
        assertThat(alice?.spouseLookupKey).isNull()
        assertThat(bob?.spouseLookupKey).isNull()
    }

    @Test
    fun unlinkCouple_doesNothing_whenContactHasNoSpouse() = runTest {
        // Arrange – Kontakt ohne Spouse
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))

        // Act – sollte keine Exception werfen
        repository.unlinkCouple("alice")

        // Assert – kein Seiteneffekt
        val alice = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(alice?.spouseLookupKey).isNull()
    }

    @Test
    fun unlinkCouple_doesNothing_whenContactNotInDb() = runTest {
        // Act – Kontakt existiert nicht, keine Exception erwartet
        repository.unlinkCouple("nonexistent_key")
        // Kein Assert notwendig – Test besteht, wenn keine Exception geworfen wird
    }

    // ─────────────────────────────────────────────────
    // Kombination: Link → Unlink → Re-Link
    // ─────────────────────────────────────────────────

    @Test
    fun linkUnlinkLink_resultsInConsistentFinalState() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))

        // Act – vollständiger Lebenszyklus
        repository.linkAsCouple("alice", "bob")
        repository.unlinkCouple("alice")
        repository.linkAsCouple("alice", "bob")

        // Assert – finaler Zustand: verknüpft in beiden DBs
        val aliceData = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val bobData = settingsDb.contactUserDataDao().getUserDataForContact("bob")
        val aliceContact = appDb.contactDao().getContactByLookupKey("alice")
        val bobContact = appDb.contactDao().getContactByLookupKey("bob")

        assertThat(aliceData?.spouseLookupKey).isEqualTo("bob")
        assertThat(bobData?.spouseLookupKey).isEqualTo("alice")
        assertThat(aliceContact?.spouseLookupKey).isEqualTo("bob")
        assertThat(bobContact?.spouseLookupKey).isEqualTo("alice")
    }

    @Test
    fun settingsDb_andAppDb_areConsistentAfterLink() = runTest {
        // Dieser Test verifiziert explizit, dass SettingsDB (Quelle der Wahrheit)
        // und AppDB (Cache) nach linkAsCouple denselben Zustand zeigen.
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))

        // Act
        repository.linkAsCouple("alice", "bob")

        // Assert – beide DBs zeigen denselben Spouse-Key
        val settingsAlice = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val cacheAlice = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(settingsAlice?.spouseLookupKey).isEqualTo(cacheAlice?.spouseLookupKey)

        val settingsBob = settingsDb.contactUserDataDao().getUserDataForContact("bob")
        val cacheBob = appDb.contactDao().getContactByLookupKey("bob")
        assertThat(settingsBob?.spouseLookupKey).isEqualTo(cacheBob?.spouseLookupKey)
    }

    @Test
    fun settingsDb_andAppDb_areConsistentAfterUnlink() = runTest {
        // Arrange
        appDb.contactDao().upsertContact(makeContact("alice", "Alice"))
        appDb.contactDao().upsertContact(makeContact("bob", "Bob"))
        repository.linkAsCouple("alice", "bob")

        // Act
        repository.unlinkCouple("alice")

        // Assert
        val settingsAlice = settingsDb.contactUserDataDao().getUserDataForContact("alice")
        val cacheAlice = appDb.contactDao().getContactByLookupKey("alice")
        assertThat(settingsAlice?.spouseLookupKey).isEqualTo(cacheAlice?.spouseLookupKey)

        val settingsBob = settingsDb.contactUserDataDao().getUserDataForContact("bob")
        val cacheBob = appDb.contactDao().getContactByLookupKey("bob")
        assertThat(settingsBob?.spouseLookupKey).isEqualTo(cacheBob?.spouseLookupKey)
    }
}
