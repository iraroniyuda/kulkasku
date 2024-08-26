package com.rmdev.kulkasku.ui

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var profileImageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Check for storage permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
        }

        loadUserProfile()

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    profileImageUri = uri
                    val filePath = getPathFromUri(uri)
                    if (filePath != null) {
                        val file = File(filePath)
                        if (file.exists()) {
                            compressImage(file) { compressedFile ->
                                compressedFile?.let {
                                    profileImageUri = Uri.fromFile(it)
                                    binding.profileImageView.setImageURI(profileImageUri)
                                }
                            }
                        } else {
                            Toast.makeText(context, "Failed to get image file", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to get image file path", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.changeProfilePictureButton.setOnClickListener {
            pickImage()
        }

        binding.saveButton.setOnClickListener {
            saveProfile()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.usernameEditText.setText(document.getString("username"))
                    binding.phoneNumberEditText.setText(document.getString("phoneNumber"))
                    val profilePictureUrl = document.getString("profilePictureUrl")
                    if (!profilePictureUrl.isNullOrEmpty()) {
                        Glide.with(this).load(profilePictureUrl).into(binding.profileImageView)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return
        val username = binding.usernameEditText.text.toString()
        val phoneNumber = binding.phoneNumberEditText.text.toString()

        val userUpdates = hashMapOf(
            "username" to username,
            "phoneNumber" to phoneNumber
        )

        val localProfileImageUri = profileImageUri
        if (localProfileImageUri != null) {
            val filePath = getPathFromUri(localProfileImageUri)
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists()) {
                    compressImage(file) { compressedFile ->
                        if (compressedFile != null) {
                            val profileImageRef = storage.reference.child("profile_pictures/$userId")
                            profileImageRef.putFile(Uri.fromFile(compressedFile)).continueWithTask { task ->
                                if (!task.isSuccessful) {
                                    task.exception?.let { throw it }
                                }
                                profileImageRef.downloadUrl
                            }.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    userUpdates["profilePictureUrl"] = task.result.toString()
                                    updateUserProfile(userId, userUpdates)
                                } else {
                                    Toast.makeText(context, "Failed to upload profile picture", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Failed to compress profile picture", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Failed to get image file", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Failed to get image file path", Toast.LENGTH_SHORT).show()
            }
        } else {
            updateUserProfile(userId, userUpdates)
        }
    }

    private fun updateUserProfile(userId: String, userUpdates: Map<String, Any>) {
        firestore.collection("users").document(userId).update(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                // Navigate back to ProfileFragment
                findNavController().navigate(R.id.action_editProfileFragment_to_profileFragment)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun compressImage(file: File, onComplete: (File?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                // Decode the file to a Bitmap
                val bitmap = BitmapFactory.decodeFile(file.path)
                val exif = ExifInterface(file.path)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val rotatedBitmap = rotateBitmap(bitmap, orientation)

                // Calculate the height to maintain the aspect ratio
                val targetWidth = 150
                val targetHeight = (rotatedBitmap.height.toFloat() / rotatedBitmap.width * targetWidth).toInt()

                // Resize the Bitmap
                val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, targetHeight, true)

                // Save the resized Bitmap to a file
                val compressedFile = File(requireContext().cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
                FileOutputStream(compressedFile).use { out ->
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out) // Adjust the quality as needed
                }

                onComplete(compressedFile)
            } catch (e: IOException) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getPathFromUri(uri: Uri): String? {
        val context = context ?: return null
        var path: String? = null
        if (DocumentsContract.isDocumentUri(context, uri)) {
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
                    if (id.startsWith("raw:")) {
                        return id.removePrefix("raw:")
                    }
                    val contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), id.toLongOrNull() ?: return null
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
            return getDataColumn(uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return path
    }

    private fun getDataColumn(uri: Uri?, selection: String?, selectionArgs: Array<String>?): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = requireContext().contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                if (index != -1) {
                    return cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
