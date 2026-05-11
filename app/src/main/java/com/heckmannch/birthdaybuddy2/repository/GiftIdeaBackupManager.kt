package com.heckmannch.birthdaybuddy2.repository

import android.util.Log
import com.heckmannch.birthdaybuddy2.database.ContactDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GiftIdeaBackupManager @Inject constructor(
    private val contactDao: ContactDao,
) {
    /**
     * Exportiert alle Kontakte mit Geschenkideen als JSON-String.
     */
    suspend fun exportGiftIdeas(): String = withContext(Dispatchers.IO) {
        val contacts = contactDao.getAllContactsImmediate().filter { !it.giftIdeas.isNullOrBlank() }
        val root = JSONArray()
        contacts.forEach { contact ->
            val obj = JSONObject().apply {
                put("lookupKey", contact.lookupKey)
                put("fullName", contact.fullName)
                put("giftIdeas", contact.giftIdeas)
            }
            root.put(obj)
        }
        root.toString(2)
    }

    /**
     * Importiert Geschenkideen aus einem JSON-String und ordnet sie via LookupKey oder Name zu.
     */
    suspend fun importGiftIdeas(jsonString: String): Int = withContext(Dispatchers.IO) {
        try {
            val root = JSONArray(jsonString)
            if (root.length() == 0) return@withContext 0

            val dbContacts = contactDao.getAllContactsImmediate()
            val contactsByLookup = dbContacts.associateBy { it.lookupKey }
            val contactsByName = dbContacts.associateBy { it.fullName }
            var count = 0

            for (i in 0 until root.length()) {
                val obj = root.getJSONObject(i)
                val lookupKey = obj.optString("lookupKey")
                val giftIdeas = obj.optString("giftIdeas")
                val fullName = obj.optString("fullName")

                if (giftIdeas.isNullOrBlank()) continue

                // Match via LookupKey (Best) oder Name (Fallback)
                val target = contactsByLookup[lookupKey] ?: contactsByName[fullName]

                if (target != null) {
                    contactDao.upsertContact(target.copy(giftIdeas = giftIdeas))
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
