package com.rmdev.kulkasku.ui.profile


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.rmdev.kulkasku.R
import java.text.SimpleDateFormat
import java.util.*



class CommentsAdapter(private var comments: List<Comment>) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = view.findViewById(R.id.commentProfileImageView)
        val usernameTextView: TextView = view.findViewById(R.id.commentUsernameTextView)
        val timestampTextView: TextView = view.findViewById(R.id.commentTimestampTextView)
        val contentTextView: TextView = view.findViewById(R.id.commentContentTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.usernameTextView.text = comment.username
        holder.timestampTextView.text = formatTimestamp(comment.timestamp)
        holder.contentTextView.text = comment.content

        comment.profileImageUrl?.let {
            Glide.with(holder.profileImageView.context)
                .load(it)
                .transform(CircleCrop())
                .into(holder.profileImageView)
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    fun updateComments(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
