package com.rmdev.kulkasku.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreData {
    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    val usersCollection = firestore.collection("users")
    val currentUserId: String? get() = FirebaseAuth.getInstance().currentUser?.uid
}