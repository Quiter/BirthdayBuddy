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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val json = JsonUtils.prettyJson

    /**
     * Exports all contacts with gift ideas as a JSON string.
     * Uses the [ContactUserData] table as the primary source.
     */
    suspend fun exportGiftIdeas(): String = withContext(ioDispatcher) {
        val userDataList =
            contactUserDataDao.getAllUserDataImmediate().filter { it.giftIdeas.isNotEmpty() }
        val dbContacts = contactDao.getAllContactsImmediate().associateBy { it.lookupKey }

        val entries = userDataList.map { userData ->
            val contact = dbContacts[userData.lookupKey]
            GiftIdeaBackupEntry(
                lookupKey = userData.lookupKey,
                fullName = contact?.fullName ?: "",
                giftIdeas = userData.giftIdeas
            )
        }

        json.encodeToString(entries)
    }

    /**
     * Imports gift ideas from a JSON string.
     * Writes the imported data into the persistent [ContactUserData] table.
     */
    suspend fun importGiftIdeas(jsonString: String): Int = withContext(ioDispatcher) {
        try {
            val rootElement = json.parseToJsonElement(jsonString)
            if (rootElement !is JsonArray || rootElement.isEmpty()) return@withContext 0

            val dbContacts = contactDao.getAllContactsImmediate()
            val contactsByLookup = dbContacts.associateBy { it.lookupKey }
            val contactsByName = dbContacts.associateBy { it.fullName }
            val converters = GiftIdeaConverters()
            settingsDatabase.withTransaction {
                val existingUserDataMap =
                    contactUserDataDao.getAllUserDataImmediate().associateBy { it.lookupKey }

                val toUpsert = mutableListOf<ContactUserData>()

                for (element in rootElement) {
                    if (element !is JsonObject) continue

                    val lookupKey = element["lookupKey"]?.jsonPrimitive?.contentOrNull ?: ""
                    val fullName = element["fullName"]?.jsonPrimitive?.contentOrNull ?: ""
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
                        is JsonElement -> {
                            val str = giftIdeasElement.jsonPrimitive.contentOrNull
                            if (!str.isNullOrBlank()) {
                                converters.toGiftIdeaList(str)
                            } else {
                                emptyList()
                            }
                        }
                        null -> emptyList()
                    }

                    if (giftIdeas.isEmpty()) continue

                    // Match via LookupKey (best) or name (fallback)
                    val targetLookupKey = contactsByLookup[lookupKey]?.lookupKey
                        ?: contactsByName[fullName]?.lookupKey

                    if (targetLookupKey != null) {
                        val existingUserData = existingUserDataMap[targetLookupKey]
                        toUpsert.add(
                            ContactUserData(
                                lookupKey = targetLookupKey,
                                giftIdeas = giftIdeas,
                                spouseLookupKey = existingUserData?.spouseLookupKey
                            )
                        )
                    }
                }

                if (toUpsert.isNotEmpty()) {
                    contactUserDataDao.upsertUserDataList(toUpsert)
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
