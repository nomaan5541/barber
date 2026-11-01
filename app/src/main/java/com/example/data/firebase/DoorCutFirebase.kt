package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Shared Firebase service singleton for DoorCut.
 *
 * Ensures every screen, ViewModel, and repository shares a single FirebaseAuth
 * instance and always connects to the provisioned named Cloud Firestore database
 * (`R.string.firestore_database_id`) rather than creating scattered instances.
 */
object DoorCutFirebase {

    @Volatile
    private var cachedFirestore: FirebaseFirestore? = null

    fun auth(): FirebaseAuth = FirebaseAuth.getInstance()

    fun firestore(context: Context): FirebaseFirestore {
        cachedFirestore?.let { return it }
        return synchronized(this) {
            cachedFirestore ?: FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            ).also { instance ->
                cachedFirestore = instance
            }
        }
    }
}
