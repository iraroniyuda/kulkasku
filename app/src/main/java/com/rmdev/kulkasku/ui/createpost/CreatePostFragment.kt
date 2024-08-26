package com.rmdev.kulkasku.ui.createpost

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.Config
import com.bumptech.glide.Glide
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentCreatePostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.default
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    private var videoUri: Uri? = null
    private var videoCompressed: Boolean = false
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Uri>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupActivityResultLaunchers()

        binding.addImageButton.setOnClickListener {
            pickImage()
        }

        binding.addVideoButton.setOnClickListener {
            pickVideo()
        }

        binding.takePictureButton.setOnClickListener {
            if (checkPermissions()) {
                takePicture()
            }
        }

        binding.recordVideoButton.setOnClickListener {
            if (checkPermissions()) {
                recordVideo()
            }
        }

        binding.postButton.setOnClickListener {
            hideKeyboard()
            hideButtons()
            Log.d("CreatePostFragment", "Post button clicked")
            checkCreatorRoleAndPost()
        }
    }

    private fun setupActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUri = uri
                    val file = File(getPathFromUri(uri) ?: return@let)
                    compressImage(file) { compressedFile ->
                        compressedFile?.let { displayMedia(Uri.fromFile(it)) }
                    }
                }
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri?.let {
                    Log.d("CreatePostFragment", "Image captured successfully: $it")
                    val file = File(getPathFromUri(it) ?: return@let)
                    // Show loading or placeholder thumbnail immediately
                    displayMedia(it)
                    compressImage(file) { compressedFile ->
                        compressedFile?.let { displayMedia(Uri.fromFile(it)) }
                    }
                } ?: run {
                    Log.e("CreatePostFragment", "Image URI is null after capture")
                }
            } else {
                Log.e("CreatePostFragment", "Image capture failed")
            }
        }

        videoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    handleVideoResult(uri)
                }
            }
        }

        takeVideoLauncher = registerForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
            if (success) {
                videoUri?.let { handleVideoResult(it) }
            }
        }
    }

    private fun compressImage(file: File, onComplete: (File?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val compressedImageFile = Compressor.compress(requireContext(), file) {
                    quality(50) // Adjust the quality as needed
                    format(Bitmap.CompressFormat.JPEG) // Choose format
                }
                Log.d("Image Compression", "Original size: ${file.length()} bytes, Compressed size: ${compressedImageFile.length()} bytes")
                onComplete(compressedImageFile)
            } catch (e: IOException) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }

    private fun displayMedia(uri: Uri?) {
        uri?.let {
            binding.mediaPreviewImageView.visibility = View.VISIBLE
            Glide.with(this)
                .load(it)
                .error(R.drawable.ic_error)
                .into(binding.mediaPreviewImageView)
            Log.d("CreatePostFragment", "Media displayed: $uri")
        } ?: run {
            Log.e("CreatePostFragment", "Failed to display media: URI is null")
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun pickVideo() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        videoPickerLauncher.launch(intent)
    }

    private fun takePicture() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile: File = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )

        val localImageUri = FileProvider.getUriForFile(
            requireContext(),
            "com.gbdev.ghostbarber.fileprovider",
            photoFile
        )

        imageUri = localImageUri

        takePictureLauncher.launch(localImageUri)
    }

    private fun recordVideo() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFile = File.createTempFile(
            "MP4_${timeStamp}_",
            ".mp4",
            storageDir
        )

        val localVideoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.gbdev.ghostbarber.fileprovider",
            videoFile
        )

        videoUri = localVideoUri
        videoCompressed = false
        takeVideoLauncher.launch(localVideoUri)
    }

    private fun handleVideoResult(uri: Uri?) {
        uri?.let {
            val compressedVideoFilePath = File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES), "compressed_${System.currentTimeMillis()}.mp4").absolutePath
            compressVideo(it, compressedVideoFilePath) { compressedUri ->
                if (compressedUri != null) {
                    videoCompressed = true
                    videoUri = compressedUri
                    displayMedia(compressedUri)
                    Toast.makeText(context, "Video compression successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Video compression failed", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            }
        }
    }

    private fun compressVideo(inputUri: Uri, outputFilePath: String, onComplete: (Uri?) -> Unit) {
        var inputFilePath = getPathFromUri(inputUri)
        if (inputFilePath == null) {
            Log.e("CompressVideo", "Failed to get the file path from URI, using InputStream directly")
            // Fall back to using InputStream
            val inputStream = context?.contentResolver?.openInputStream(inputUri)
            if (inputStream != null) {
                val tempFile = File.createTempFile("temp_video", ".mp4", context?.cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                inputFilePath = tempFile.absolutePath
            } else {
                onComplete(null)
                return
            }
        }

        val command = arrayOf(
            "-i", inputFilePath,
            "-vcodec", "libx264",
            "-crf", "28",            // Adjust CRF for quality/size balance
            "-preset", "ultrafast",     // Adjust preset for encoding speed
            "-vf", "scale=720:1280", // Adjust resolution (example: 720p)
            "-b:v", "1000k",         // Adjust bitrate (example: 1000 kbps)
            "-r", "24",
            outputFilePath
        )

        FFmpeg.executeAsync(command) { _, returnCode ->
            if (returnCode == Config.RETURN_CODE_SUCCESS) {
                Log.d("FFmpeg", "Video compression successful")
                onComplete(Uri.fromFile(File(outputFilePath)))
            } else {
                Log.e("FFmpeg", "Video compression failed with return code: $returnCode")
                onComplete(null)
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        // Check for newer API levels and Document URIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            when {
                isExternalStorageDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    if ("primary".equals(type, ignoreCase = true)) {
                        return "${Environment.getExternalStorageDirectory()}/${split[1]}"
                    }
                }
                isDownloadsDocument(uri) -> {
                    val id = DocumentsContract.getDocumentId(uri)
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), id.toLong()
                    )
                    return getDataColumn(contentUri, null, null)
                }
                isMediaDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]
                    var contentUri: Uri? = null
                    when (type) {
                        "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    return getDataColumn(contentUri, selection, selectionArgs)
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            // Return the remote address
            return getDataColumn(uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun getDataColumn(uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context?.contentResolver?.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(column)
                if (index != -1) {
                    return cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            Log.e("getDataColumn", "Failed to get data column from URI", e)
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun uploadVideoToFirebase(uri: Uri?) {
        val localUri = uri ?: return
        val inputStream = context?.contentResolver?.openInputStream(localUri) ?: return

        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("videos/$fileName.mp4")

        ref.putStream(inputStream)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveVideoInfoToFirestore(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Log.e("Upload", "Failed to upload video to storage", it)
                navigateToHome()
            }
    }

    private fun saveVideoInfoToFirestore(videoUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val post = hashMapOf(
            "userId" to userId,
            "content" to binding.postContentEditText.text.toString(),
            "timestamp" to System.currentTimeMillis(),
            "mediaUrl" to videoUrl,
            "mediaType" to "video"
        )

        firestore.collection("posts").document().set(post)
            .addOnSuccessListener {
                Toast.makeText(context, "Post created successfully", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to create post", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
    }

    private fun uploadImageToFirebase(uri: Uri?) {
        val localUri = uri ?: return
        val inputStream = context?.contentResolver?.openInputStream(localUri) ?: return

        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("images/$fileName.jpg")

        ref.putStream(inputStream)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveImageInfoToFirestore(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Log.e("Upload", "Failed to upload image to storage", it)
                navigateToHome()
            }
    }

    private fun saveImageInfoToFirestore(imageUrl: String) {
        val userId = auth.currentUser?.uid ?: return
        val post = hashMapOf(
            "userId" to userId,
            "content" to binding.postContentEditText.text.toString(),
            "timestamp" to System.currentTimeMillis(),
            "mediaUrl" to imageUrl,
            "mediaType" to "image"
        )

        firestore.collection("posts").document().set(post)
            .addOnSuccessListener {
                Toast.makeText(context, "Post created successfully", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to create post", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
            return false
        }
        return true
    }

    private fun checkCreatorRoleAndPost() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getBoolean("roles.creator") == true) {
                    Log.d("CreatePostFragment", "User has creator role")
                    postContent()
                } else {
                    Toast.makeText(context, "You do not have permission to post content", Toast.LENGTH_SHORT).show()
                    navigateToHome()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to check user role", Toast.LENGTH_SHORT).show()
                navigateToHome()
            }
    }

    private fun postContent() {
        Log.d("CreatePostFragment", "postContent called")
        if (!videoCompressed && videoUri != null) {
            Log.d("CreatePostFragment", "Video is not yet compressed, aborting post")
            Toast.makeText(context, "Video is still being compressed, please wait", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val content = binding.postContentEditText.text.toString()
        if (content.isEmpty() && imageUri == null && videoUri == null) {
            Toast.makeText(context, "Please add content to your post", Toast.LENGTH_SHORT).show()
            return
        }

        showLoadingBar()

        if (imageUri != null) {
            uploadImageToFirebase(imageUri)
        } else if (videoUri != null) {
            uploadVideoToFirebase(videoUri)
        }
    }

    private fun showLoadingBar() {
        binding.progressBar.visibility = View.VISIBLE
        binding.uploadingText.visibility = View.VISIBLE
        val animator = ObjectAnimator.ofInt(binding.progressBar, "progress", 0, 100)
        animator.duration = 8000 // 10 seconds
        animator.start()
    }

    private fun hideLoadingBar() {
        binding.progressBar.visibility = View.GONE
        binding.uploadingText.visibility = View.GONE
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun hideButtons() {
        binding.addImageButton.visibility = View.GONE
        binding.addVideoButton.visibility = View.GONE
        binding.takePictureButton.visibility = View.GONE
        binding.recordVideoButton.visibility = View.GONE
        binding.postButton.visibility = View.GONE
    }

    private fun navigateToHome() {
        hideLoadingBar()
        //findNavController().navigate(R.id.action_createPostFragment_to_navigation_home)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
