package com.rmdev.kulkasku.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rmdev.kulkasku.databinding.FragmentCommentsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rmdev.kulkasku.ui.profile.CommentsFragmentArgs


class CommentsFragment : Fragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var commentsViewModel: CommentsViewModel
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var postId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentsViewModel = ViewModelProvider(this).get(CommentsViewModel::class.java)
        binding.viewModel = commentsViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        postId = CommentsFragmentArgs.fromBundle(requireArguments()).postId

        commentsAdapter = CommentsAdapter(emptyList())
        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.commentsRecyclerView.adapter = commentsAdapter

        commentsViewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentsAdapter.updateComments(comments)
        }

        commentsViewModel.loadComments(postId)

        binding.postCommentButton.setOnClickListener {
            val commentText = binding.commentEditText.text.toString()
            if (commentText.isNotEmpty()) {
                postComment(commentText)
                binding.commentEditText.text.clear()
                hideKeyboard()
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        setupUI(binding.root)
    }

    private fun postComment(content: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val username = document.getString("username") ?: "Unknown"
                    val profileImageUrl = document.getString("profilePicture") ?: ""

                    val newComment = hashMapOf(
                        "userId" to userId,
                        "postId" to postId,
                        "content" to content,
                        "timestamp" to System.currentTimeMillis(),
                        "username" to username,
                        "profileImageUrl" to profileImageUrl
                    )

                    firestore.collection("posts").document(postId).collection("comments").add(newComment)
                }
            }
    }

    private fun setupUI(view: View) {
        // Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideKeyboard()
                false
            }
        }

        // If a layout container, iterate over children and set up touch listener.
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    private fun hideKeyboard() {
        activity?.let { activity ->
            val inputMethodManager = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.currentFocus?.windowToken, 0)
            binding.commentEditText.clearFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}