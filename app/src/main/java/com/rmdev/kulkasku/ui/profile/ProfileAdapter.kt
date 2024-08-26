package com.rmdev.kulkasku.ui.profile

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.models.Post
import com.rmdev.kulkasku.models.User
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ProfileAdapter(
    private val context: Context,
    private var user: User?,
    private val posts: MutableList<Post>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_PROFILE = 0
        private const val VIEW_TYPE_POST = 1
    }

    class ProfileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val emailTextView: TextView = view.findViewById(R.id.emailTextView)
        val phoneNumberTextView: TextView = view.findViewById(R.id.phoneNumberTextView)
        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val editProfileButton: ImageButton = view.findViewById(R.id.editProfileButton)
        val userSettingButton: ImageButton = view.findViewById(R.id.userSettingButton)
    }

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = view.findViewById(R.id.postProfileImageView)
        val usernameTextView: TextView = view.findViewById(R.id.postUsernameTextView)
        val timestampTextView: TextView = view.findViewById(R.id.postTimestampTextView)
        val contentTextView: TextView = view.findViewById(R.id.contentTextView)
        val mediaImageView: ImageView = view.findViewById(R.id.mediaImageView)
        val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
        val videoView: PlayerView = view.findViewById(R.id.videoView)
        val likeButton: ImageButton = view.findViewById(R.id.likeButton)
        val commentButton: ImageButton = view.findViewById(R.id.commentButton)
        val likesTextView: TextView = view.findViewById(R.id.likesTextView)
        val commentsTextView: TextView = view.findViewById(R.id.commentsTextView)
        val actionButtonsContainer: LinearLayout = view.findViewById(R.id.actionButtonsContainer)
        var exoPlayer: ExoPlayer? = null
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_PROFILE else VIEW_TYPE_POST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == VIEW_TYPE_PROFILE) R.layout.item_profile else R.layout.item_post
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return if (viewType == VIEW_TYPE_PROFILE) ProfileViewHolder(view) else PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return posts.size + 1 // +1 for the profile view
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ProfileViewHolder) {
            bindProfileViewHolder(holder)
        } else if (holder is PostViewHolder) {
            bindPostViewHolder(holder, position - 1)
        }
    }

    private fun bindProfileViewHolder(holder: ProfileViewHolder) {
        user?.let {
            holder.usernameTextView.text = it.username
            holder.emailTextView.text = it.email
            holder.phoneNumberTextView.text = it.phoneNumber
            Glide.with(context)
                .load(it.profilePictureUrl)
                .transform(CircleCrop())
                .into(holder.profileImageView)

            holder.editProfileButton.setOnClickListener {
                holder.itemView.findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            }

            holder.userSettingButton.setOnClickListener {
                holder.itemView.findNavController().navigate(R.id.action_profileFragment_to_userSettingFragment)
            }
        }
    }

    private fun bindPostViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.usernameTextView.text = post.username
        holder.timestampTextView.text = formatTimestamp(post.timestamp)
        holder.contentTextView.text = post.content
        holder.likesTextView.text = post.likesCount.toString()
        holder.commentsTextView.text = post.commentsCount.toString()

        Glide.with(context)
            .load(post.profilePictureUrl)
            .transform(CircleCrop())
            .into(holder.profileImageView)

        handleMedia(holder, post)

        holder.likeButton.setOnClickListener { toggleLike(post) }
        holder.commentButton.setOnClickListener { navigateToComments(post.id, holder.itemView) }
    }

    private fun handleMedia(holder: PostViewHolder, post: Post) {
        if (post.mediaUrl.isNullOrEmpty()) {
            holder.mediaImageView.visibility = View.GONE
            holder.videoContainer.visibility = View.GONE
            val params = holder.actionButtonsContainer.layoutParams as ConstraintLayout.LayoutParams
            params.topToBottom = R.id.contentTextView
            holder.actionButtonsContainer.layoutParams = params
        } else {
            if (post.mediaType == "video") {
                holder.mediaImageView.visibility = View.GONE
                holder.videoContainer.visibility = View.VISIBLE

                val exoPlayer = ExoPlayer.Builder(context).build()
                holder.videoView.player = exoPlayer
                val mediaItem = MediaItem.fromUri(Uri.parse(post.mediaUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()

                exoPlayer.addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        val videoFormat = exoPlayer.videoFormat
                        if (videoFormat != null) {
                            val videoWidth = videoFormat.width
                            val videoHeight = videoFormat.height
                            if (videoWidth > 0 && videoHeight > 0) {
                                val aspectRatio = videoHeight.toFloat() / videoWidth
                                val layoutParams = holder.videoView.layoutParams
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                                layoutParams.height = (holder.videoView.width * aspectRatio).toInt()
                                holder.videoView.layoutParams = layoutParams
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED) {
                            exoPlayer.setMediaItem(mediaItem)
                            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                            exoPlayer.prepare()
                        }
                    }
                })

                holder.exoPlayer = exoPlayer

                val params = holder.actionButtonsContainer.layoutParams as ConstraintLayout.LayoutParams
                params.topToBottom = R.id.videoContainer
                holder.actionButtonsContainer.layoutParams = params
            } else {
                holder.videoContainer.visibility = View.GONE
                holder.mediaImageView.visibility = View.VISIBLE
                Glide.with(context).load(post.mediaUrl).into(holder.mediaImageView)

                val params = holder.actionButtonsContainer.layoutParams as ConstraintLayout.LayoutParams
                params.topToBottom = R.id.mediaImageView
                holder.actionButtonsContainer.layoutParams = params
            }
        }
    }

    private fun toggleLike(post: Post) {
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
            updatePost(post)
        }
    }

    private fun navigateToComments(postId: String, view: View) {
        val bundle = Bundle().apply {
            putString("postId", postId)
        }
        view.findNavController().navigate(R.id.action_navigation_profile_to_commentsFragment, bundle)
    }

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    fun updatePost(updatedPost: Post) {
        val index = posts.indexOfFirst { it.id == updatedPost.id }
        if (index != -1) {
            posts[index] = updatedPost
            notifyItemChanged(index + 1) // Adjust position for profile view
        }
    }

    fun updateProfile(newUser: User) {
        user = newUser
        notifyItemChanged(0) // Update profile view
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PostViewHolder) {
            holder.exoPlayer?.release()
            holder.exoPlayer = null
        }
    }
}
