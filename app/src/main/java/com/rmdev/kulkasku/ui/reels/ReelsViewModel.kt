package com.rmdev.kulkasku.ui.reels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rmdev.kulkasku.models.Post
import com.google.firebase.firestore.FirebaseFirestore

class ReelsViewModel : ViewModel() {

    private val _videoPosts = MutableLiveData<List<Post>>()
    val videoPosts: LiveData<List<Post>> = _videoPosts

    private val firestore = FirebaseFirestore.getInstance()

    fun fetchVideoPosts() {
        firestore.collection("posts")
            .whereEqualTo("mediaType", "video")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val posts = documents.map { document ->
                    val post = document.toObject(Post::class.java)
                    post.id = document.id
                    post
                }
                _videoPosts.value = posts
            }
            .addOnFailureListener { e ->
                // Handle the error
            }
    }
}