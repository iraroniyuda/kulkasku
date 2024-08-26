package com.rmdev.kulkasku.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class CommentsViewModel : ViewModel() {

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val firestore = FirebaseFirestore.getInstance()

    fun loadComments(postId: String) {
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                val comments = snapshots?.map { document ->
                    val comment = document.toObject(Comment::class.java)
                    // Fetch the user details for each comment
                    firestore.collection("users").document(comment.userId).get()
                        .addOnSuccessListener { userDocument ->
                            if (userDocument != null) {
                                comment.username = userDocument.getString("username") ?: "Unknown"
                                comment.profileImageUrl = userDocument.getString("profilePictureUrl")
                                _comments.value = _comments.value // Trigger LiveData update
                            }
                        }
                    comment
                } ?: emptyList()
                _comments.value = comments
            }
    }


}