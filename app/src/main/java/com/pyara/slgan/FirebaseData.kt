package com.pyara.slgan

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Firestore access layer for SLGAN.
 *
 * Firestore's Android SDK enables local persistence by default, so reads can use
 * cached data offline and writes are queued until connectivity returns.
 * Configure Firestore Security Rules in Firebase Console before using real customer data.
 */
object FirebaseData {
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun observeProducts(
        onProducts: (List<Map<String, Any?>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                onProducts(snapshot?.documents?.map { document ->
                    document.data.orEmpty() + ("id" to document.id)
                }.orEmpty())
            }
    }

    fun saveCustomerProfile(
        uid: String,
        fields: Map<String, Any>,
        onComplete: (Exception?) -> Unit
    ) {
        db.collection("customers").document(uid)
            .set(fields)
            .addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { error -> onComplete(error) }
    }

    fun saveOrder(
        uid: String,
        orderId: String,
        fields: Map<String, Any>,
        onComplete: (Exception?) -> Unit
    ) {
        db.collection("customers").document(uid)
            .collection("orders").document(orderId)
            .set(fields)
            .addOnSuccessListener { onComplete(null) }
            .addOnFailureListener { error -> onComplete(error) }
    }
}
