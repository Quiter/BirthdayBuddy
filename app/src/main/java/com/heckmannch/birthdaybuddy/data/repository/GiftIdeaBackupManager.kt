package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GiftIdeaBackupManager @Inject constructor(
    private val contactDao: ContactDao,
    private val contactUserDataDao: ContactUserDataDao,
) {
    /**
     * Exportiert alle Kontakte mit Geschenkideen als JSON-String.
     * Nutzt nun die ContactUserData-Tabelle als Primärquelle.
     */
    suspend fun exportGiftIdeas(): String = withContext(Dispatchers.IO) {
        val userDataList = contactUserDataDao.getAllUserDataImmediate().filter { it.giftIdeas.isNotEmpty() }
        val dbContacts = contactDao.getAllContactsImmediate().associateBy { it.lookupKey }
        val converters = GiftIdeaConverters()
        
        val root = JSONArray()
        userDataList.forEach { userData ->
            val contact = dbContacts[userData.lookupKey]
            val obj = JSONObject().apply {
                put("lookupKey", userData.lookupKey)
                put("fullName", contact?.fullName ?: "")
                put("giftIdeas", converters.fromGiftIdeaList(userData.giftIdeas))
            }
            root.put(obj)
        }
        root.toString(2)
    }

    /**
     * Importiert Geschenkideen aus einem JSON-String.
     * Schreibt die Daten in die persistente UserData-Tabelle.
     */
    suspend fun importGiftIdeas(jsonString: String): Int = withContext(Dispatchers.IO) {
        try {
            val root = JSONArray(jsonString)
            if (root.length() == 0) return@withContext 0

            val dbContacts = contactDao.getAllContactsImmediate()
            val contactsByLookup = dbContacts.associateBy { it.lookupKey }
            val contactsByName = dbContacts.associateBy { it.fullName }
            val converters = GiftIdeaConverters()
            var count = 0

            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val lookupKey = obj.optString("lookupKey")
                val giftIdeasStr = obj.optString("giftIdeas")
                val fullName = obj.optString("fullName")

                if (giftIdeasStr.isNullOrBlank()) continue

                // Match via LookupKey (Best) oder Name (Fallback)
                val targetLookupKey = contactsByLookup[lookupKey]?.lookupKey 
                    ?: contactsByName[fullName]?.lookupKey

                if (targetLookupKey != null) {
                    val giftIdeas = converters.toGiftIdeaList(giftIdeasStr)
                    contactUserDataDao.upsertUserData(
                        ContactUserData(lookupKey = targetLookupKey, giftIdeas = giftIdeas)
                    )
                    count++
                }
            }
            count
        } catch (e: Exception) {
            Log.e("GiftIdeaBackupManager", "Import fehlgeschlagen", e)
            -1
        }
    }
}
