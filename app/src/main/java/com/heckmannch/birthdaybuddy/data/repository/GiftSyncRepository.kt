package com.heckmannch.birthdaybuddy.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.heckmannch.birthdaybuddy.data.local.BirthdayDao
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GiftSyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val birthdayDao: BirthdayDao
) {

    /**
     * Speichert eine Geschenkidee in Firestore, falls der Nutzer angemeldet ist.
     */
    suspend fun uploadGiftIdea(contactId: String, giftIdea: String) {
        val userId = auth.currentUser?.uid ?: return
        
        withContext(Dispatchers.IO) {
            val data = mapOf("giftIdea" to giftIdea)
            firestore.collection("users")
                .document(userId)
                .collection("gifts")
                .document(contactId)
                .set(data)
                .await()
        }
    }

    /**
     * Lädt alle Geschenkideen aus Firestore und aktualisiert die lokale DB.
     */
    suspend fun syncGiftsFromCloud() {
        val userId = auth.currentUser?.uid ?: return

        withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("gifts")
                    .get()
                    .await()

                snapshot.documents.forEach { doc ->
                    val contactId = doc.id
                    val giftIdea = doc.getString("giftIdea") ?: ""
                    if (giftIdea.isNotEmpty()) {
                        birthdayDao.updateGiftIdea(contactId, giftIdea)
                    }
                }
            } catch (e: Exception) {
                // Fehlerbehandlung (Logging etc.)
            }
        }
    }
}
