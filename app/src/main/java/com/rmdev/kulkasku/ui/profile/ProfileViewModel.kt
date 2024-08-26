package com.rmdev.kulkasku.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rmdev.kulkasku.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel : ViewModel() {

    private val _profile = MutableLiveData<Profile>()
    val profile: LiveData<Profile> = _profile

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun fetchProfileAndPosts() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val profile = document.toObject(Profile::class.java)
                    if (profile != null) {
                        _profile.value = profile
                    }
                }
            }

        firestore.collection("posts").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { documents ->
                val posts = documents.map { document ->
                    document.toObject(Post::class.java)
                }
                _posts.value = posts
            }
    }
}