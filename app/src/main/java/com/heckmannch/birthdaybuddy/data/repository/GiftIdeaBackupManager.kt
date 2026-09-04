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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GiftIdeaBackupEntry(
    val lookupKey: String,
    val fullName: String,
    val giftIdeas: List<GiftIdea>,
)

@Singleton
class GiftIdeaBackupManager @Inject constructor(
    private val contactDao: ContactDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val settingsDatabase: SettingsDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Exportiert alle Kontakte mit Geschenkideen als JSON-String.
     * Nutzt nun die ContactUserData-Tabelle als Primärquelle.
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
     * Importiert Geschenkideen aus einem JSON-String.
     * Schreibt die Daten in die persistente UserData-Tabelle.
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
                            } catch (_: Exception) {
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

                    // Match via LookupKey (Best) oder Name (Fallback)
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
            Log.e("GiftIdeaBackupManager", "Import fehlgeschlagen", e)
            -1
        }
    }
}
