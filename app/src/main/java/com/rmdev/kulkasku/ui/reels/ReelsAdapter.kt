package com.rmdev.kulkasku.ui.reels

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ReelsAdapter(private val context: Context, private val posts: MutableList<Post>) :
    RecyclerView.Adapter<ReelsAdapter.ReelsViewHolder>() {

    private var recyclerView: RecyclerView? = null
    private var currentPlayingViewHolder: ReelsViewHolder? = null

    class ReelsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerView: PlayerView = view.findViewById(R.id.playerView)
        val usernameTextView: TextView = view.findViewById(R.id.postUsernameTextView)
        val likeButton: ImageButton = view.findViewById(R.id.likeButton)
        val likesTextView: TextView = view.findViewById(R.id.likesTextView)
        val commentButton: ImageButton = view.findViewById(R.id.commentButton)
        val commentsTextView: TextView = view.findViewById(R.id.commentsTextView)
        val backButton: ImageButton = view.findViewById(R.id.backButton)
        var player: ExoPlayer? = null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReelsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reel, parent, false)
        return ReelsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReelsViewHolder, position: Int) {
        if (posts.isEmpty()) return // Prevent division by zero

        // Get the actual position by modding with the size of the list
        val actualPosition = position % posts.size
        val post = posts[actualPosition]
        holder.usernameTextView.text = post.username
        holder.likesTextView.text = post.likesCount.toString()
        holder.commentsTextView.text = post.commentsCount.toString()

        val mediaItem = MediaItem.fromUri(Uri.parse(post.mediaUrl))
        holder.player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(mediaItem)
            repeatMode = ExoPlayer.REPEAT_MODE_ALL // Set repeat mode to loop the video
            prepare()
        }
        holder.playerView.player = holder.player

        holder.likeButton.setOnClickListener {
            toggleLike(post, holder)
        }

        holder.commentButton.setOnClickListener {
            navigateToComments(post.id, holder)
        }

        holder.backButton.setOnClickListener {
            navigateBackToHome(holder)
        }

        // Bring the like button and comment button to the front
        holder.likeButton.bringToFront()
        holder.commentButton.bringToFront()
        holder.backButton.bringToFront()

        // Make sure PlayerView doesn't intercept touch events meant for likeButton, commentButton, or backButton
        holder.playerView.useController = false
        holder.playerView.setOnTouchListener { _, _ -> true } // Disable touch events on PlayerView

        if (position == 0) {
            currentPlayingViewHolder = holder
            holder.player?.playWhenReady = true
        }
    }

    override fun onViewRecycled(holder: ReelsViewHolder) {
        super.onViewRecycled(holder)
        holder.player?.release()
        holder.player = null
    }

    override fun getItemCount(): Int {
        // Return a large number to simulate infinite scrolling
        return if (posts.isEmpty()) 0 else Integer.MAX_VALUE
    }

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun getPostAt(position: Int): Post? {
        return if (posts.isNotEmpty()) posts[position % posts.size] else null
    }

    fun playPlayerAt(position: Int) {
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position) as? ReelsViewHolder
        currentPlayingViewHolder?.player?.playWhenReady = false
        viewHolder?.player?.playWhenReady = true
        currentPlayingViewHolder = viewHolder
    }

    fun releasePlayerAt(position: Int) {
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position) as? ReelsViewHolder
        viewHolder?.player?.release()
        viewHolder?.player = null
    }

    private fun toggleLike(post: Post, holder: ReelsViewHolder) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val postRef = FirebaseFirestore.getInstance().collection("posts").document(post.id)
        val likesRef = postRef.collection("likes")
        val likeDocRef = likesRef.document(userId)

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(likeDocRef)
            if (snapshot.exists()) {
                transaction.delete(likeDocRef)
                post.likesCount -= 1
            } else {
                val newLike = hashMapOf(
                    "userId" to userId,
                    "postId" to post.id
                )
                transaction.set(likeDocRef, newLike)
                post.likesCount += 1
            }
            transaction.update(postRef, "likesCount", post.likesCount)
        }.addOnSuccessListener {
            holder.likesTextView.text = post.likesCount.toString()
        }
    }

    private fun navigateToComments(postId: String, holder: ReelsViewHolder) {
        val bundle = Bundle().apply {
            putString("postId", postId)
        }
        holder.itemView.findNavController().navigate(R.id.action_reelsFragment_to_commentsFragment, bundle)

        // Add a listener for when the user returns from the comments fragment to update the comments count
        holder.itemView.findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("commentAdded")?.observe(holder.itemView.findNavController().currentBackStackEntry!!) { commentAdded ->
            if (commentAdded) {
                updateCommentsCount(postId, holder)
            }
        }
    }


    private fun navigateBackToHome(holder: ReelsViewHolder) {
        holder.itemView.findNavController().navigate(R.id.navigation_home, null, NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build())
    }

    private fun updateCommentsCount(postId: String, holder: ReelsViewHolder) {
        val postRef = FirebaseFirestore.getInstance().collection("posts").document(postId)
        postRef.collection("comments").get()
            .addOnSuccessListener { comments ->
                val commentsCount = comments.size()
                postRef.update("commentsCount", commentsCount)
                    .addOnSuccessListener {
                        val post = getPostAt(holder.adapterPosition)
                        post?.commentsCount = commentsCount
                        holder.commentsTextView.text = commentsCount.toString()
                    }
            }
    }
}
