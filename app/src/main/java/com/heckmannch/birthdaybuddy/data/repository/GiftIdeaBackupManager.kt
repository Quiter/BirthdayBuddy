package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.util.JsonUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data transfer object representing a single backup entry for contact gift ideas.
 */
@Serializable
data class GiftIdeaBackupEntry(
    val lookupKey: String,
    val fullName: String,
    val giftIdeas: List<GiftIdea>,
)

/**
 * Manages the export and import of gift ideas to and from JSON format,
 * persisting imported entries to the persistent [ContactUserData] database table.
 */
@Singleton
class GiftIdeaBackupManager @Inject constructor(
    private val contactDao: ContactDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val settingsDatabase: SettingsDatabase,
    private val giftIdeaConverters: GiftIdeaConverters = GiftIdeaConverters(),
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val json = JsonUtils.prettyJson

    /**
     * Exports all contacts with gift ideas as a JSON string.
     * Uses the [ContactUserData] table as the primary source.
     */
    suspend fun exportGiftIdeas(): String = withContext(ioDispatcher) {
        val userDataList =
            contactUserDataDao.getAllUserDataImmediate().mapNotNull { userData ->
                val ideas = giftIdeaConverters.toGiftIdeaList(userData.giftIdeasJson)
                if (ideas.isEmpty()) null else userData to ideas
            }
        val dbContacts = contactDao.getAllContactsImmediate().associateBy { it.lookupKey }

        val entries = userDataList.map { (userData, ideas) ->
            val contact = dbContacts[userData.lookupKey]
            GiftIdeaBackupEntry(
                lookupKey = userData.lookupKey,
                fullName = contact?.fullName ?: "",
                giftIdeas = ideas
            )
        }

        json.encodeToString(entries)
    }

    /**
     * Imports gift ideas from a JSON string.
     * Writes the imported data into the persistent [ContactUserData] table,
     * skipping malformed entries and deduplicating multiple entries per contact.
     *
     * @param jsonString The JSON array payload exported by [exportGiftIdeas].
     * @return The number of unique contacts updated, or `-1` if the JSON payload cannot be parsed.
     */
    suspend fun importGiftIdeas(jsonString: String): Int = withContext(ioDispatcher) {
        try {
            val rootElement = json.parseToJsonElement(jsonString)
            if (rootElement !is JsonArray || rootElement.isEmpty()) return@withContext 0

            val dbContacts = contactDao.getAllContactsImmediate()
            val contactsByLookup = dbContacts
                .filter { it.lookupKey.isNotBlank() }
                .associateBy { it.lookupKey }
            val contactsByName = dbContacts
                .filter { it.fullName.isNotBlank() }
                .associateBy { it.fullName }
            settingsDatabase.withTransaction {
                val existingUserDataMap =
                    contactUserDataDao.getAllUserDataImmediate().associateBy { it.lookupKey }

                val toUpsert = LinkedHashMap<String, ContactUserData>()

                for (element in rootElement) {
                    if (element !is JsonObject) continue

                    val lookupKey = (element["lookupKey"] as? JsonPrimitive)?.contentOrNull ?: ""
                    val fullName = (element["fullName"] as? JsonPrimitive)?.contentOrNull ?: ""
                    val giftIdeasElement = element["giftIdeas"]

                    val giftIdeas: List<GiftIdea> = when (giftIdeasElement) {
                        is JsonArray -> {
                            try {
                                json.decodeFromJsonElement<List<GiftIdea>>(giftIdeasElement)
                            } catch (e: Exception) {
                                Log.w(TAG, "Fehler beim Dekodieren der Geschenkideen aus JSON-Array", e)
                                emptyList()
                            }
                        }
                        is JsonPrimitive -> {
                            val str = giftIdeasElement.contentOrNull
                            if (!str.isNullOrBlank()) {
                                giftIdeaConverters.toGiftIdeaList(str)
                            } else {
                                emptyList()
                            }
                        }
                        null -> emptyList()
                        else -> {
                            Log.w(
                                TAG,
                                "Skipping unsupported giftIdeas JSON element type: ${giftIdeasElement::class.simpleName}"
                            )
                            emptyList()
                        }
                    }

                    if (giftIdeas.isEmpty()) continue

                    // Match via LookupKey (best) or name (fallback), never matching blank values
                    val targetLookupKey = lookupKey
                        .takeIf { it.isNotBlank() }
                        ?.let { contactsByLookup[it]?.lookupKey }
                        ?: fullName
                            .takeIf { it.isNotBlank() }
                            ?.let { contactsByName[it]?.lookupKey }

                    if (targetLookupKey != null) {
                        val existingUserData = existingUserDataMap[targetLookupKey]
                        toUpsert[targetLookupKey] = ContactUserData(
                            lookupKey = targetLookupKey,
                            giftIdeasJson = giftIdeaConverters.fromGiftIdeaList(giftIdeas),
                            spouseLookupKey = existingUserData?.spouseLookupKey
                        )
                    }
                }

                if (toUpsert.isNotEmpty()) {
                    contactUserDataDao.upsertUserDataList(toUpsert.values.toList())
                }
                toUpsert.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            -1
        }
    }

    companion object {
        private const val TAG = "GiftIdeaBackupManager"
    }
}
