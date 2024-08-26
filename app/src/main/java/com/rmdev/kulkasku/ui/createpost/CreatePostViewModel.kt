package com.rmdev.kulkasku.ui.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData

class CreatePostViewModel : ViewModel() {
    val postContent = MutableLiveData<String>()

    fun setPostContent(content: String) {
        postContent.value = content
    }
}