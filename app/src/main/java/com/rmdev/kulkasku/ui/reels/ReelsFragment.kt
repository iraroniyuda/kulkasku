package com.rmdev.kulkasku.ui.reels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.rmdev.kulkasku.databinding.FragmentReelsBinding
import com.rmdev.kulkasku.models.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.rmdev.kulkasku.MainActivity

class ReelsFragment : Fragment() {

    private lateinit var reelsViewModel: ReelsViewModel
    private var _binding: FragmentReelsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var reelsAdapter: ReelsAdapter
    private var currentReelPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        reelsViewModel = ViewModelProvider(this)[ReelsViewModel::class.java]
        _binding = FragmentReelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupObservers()


        reelsViewModel.fetchVideoPosts()

        // Handle navigation result for comment added
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("commentAdded")
            ?.observe(viewLifecycleOwner) { commentAdded ->
                if (commentAdded) {
                    updateCommentsCount()
                }
            }
    }

    private fun setupRecyclerView() {
        reelsAdapter = ReelsAdapter(requireContext(), mutableListOf())
        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        binding.reelsRecyclerView.layoutManager = layoutManager
        binding.reelsRecyclerView.adapter = reelsAdapter

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.reelsRecyclerView)

        // Save the current position when the user scrolls
        binding.reelsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val newReelPosition = layoutManager.findFirstVisibleItemPosition()
                if (newReelPosition != currentReelPosition) {
                    reelsAdapter.playPlayerAt(newReelPosition)
                    currentReelPosition = newReelPosition
                }
            }
        })
    }

    private fun setupObservers() {
        reelsViewModel.videoPosts.observe(viewLifecycleOwner) { posts ->
            fetchCommentsCounts(posts) { updatedPosts ->
                reelsAdapter.updatePosts(updatedPosts)
                // Restore the current position if necessary
                binding.reelsRecyclerView.scrollToPosition(currentReelPosition)
            }
        }
    }

    private fun fetchCommentsCounts(posts: List<Post>, onComplete: (List<Post>) -> Unit) {
        val updatedPosts = posts.toMutableList()
        var remainingTasks = updatedPosts.size

        updatedPosts.forEach { post ->
            firestore.collection("posts").document(post.id).collection("comments").get()
                .addOnSuccessListener { comments ->
                    post.commentsCount = comments.size()
                    if (--remainingTasks == 0) {
                        onComplete(updatedPosts)
                    }
                }
                .addOnFailureListener {
                    if (--remainingTasks == 0) {
                        onComplete(updatedPosts)
                    }
                }
        }
    }

    private fun updateCommentsCount() {
        val post = reelsAdapter.getPostAt(currentReelPosition)
        post?.let {
            firestore.collection("posts").document(it.id).collection("comments").get()
                .addOnSuccessListener { comments ->
                    val commentsCount = comments.size()
                    firestore.collection("posts").document(it.id)
                        .update("commentsCount", commentsCount)
                        .addOnSuccessListener {
                            post.commentsCount = commentsCount
                            reelsAdapter.notifyItemChanged(currentReelPosition)
                        }
                }
        }
    }

    override fun onPause() {
        super.onPause()
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }


    private fun releasePlayer() {
        reelsAdapter.releasePlayerAt(currentReelPosition)
    }
}