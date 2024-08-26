package com.rmdev.kulkasku.ui.usersetting

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentUserSettingBinding
import com.rmdev.kulkasku.ui.admin.AdminPanelActivity
import com.rmdev.kulkasku.ui.createpost.CreatePostFragment
import com.rmdev.kulkasku.ui.seller.SellerPanelFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserSettingFragment : Fragment() {

    private var _binding: FragmentUserSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var buttonAdminPanel: Button
    private lateinit var buttonSellerPanel: Button
    private lateinit var buttonCreatorPanel: Button
    private lateinit var buttonLogout: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        buttonAdminPanel = binding.buttonAdminPanel
        buttonSellerPanel = binding.buttonSellerPanel
        buttonCreatorPanel = binding.buttonCreatorPanel
        buttonLogout = binding.buttonLogout

        buttonAdminPanel.visibility = View.GONE
        buttonSellerPanel.visibility = View.GONE
        buttonCreatorPanel.visibility = View.GONE

        val userId = auth.currentUser?.uid ?: return
        checkUserRoles(userId)

        buttonAdminPanel.setOnClickListener {
            val intent = Intent(requireContext(), AdminPanelActivity::class.java)
            startActivity(intent)
        }

        buttonSellerPanel.setOnClickListener {
            findNavController().navigate(R.id.action_userSettingFragment_to_sellerPanelFragment)
        }

        buttonCreatorPanel.setOnClickListener {
            findNavController().navigate(R.id.action_userSettingFragment_to_createPostFragment)
        }

        buttonLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Logout") { _, _ -> performLogout() }
                .show()
        }

        binding.backToHomeButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun performLogout() {
        auth.signOut()
        findNavController().navigate(R.id.loginFragment)
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun checkUserRoles(userId: String) {
        val userDoc = firestore.collection("users").document(userId)
        userDoc.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val isAdmin = document.getBoolean("isAdmin") ?: false
                val isSeller = document.getBoolean("roles.seller") ?: false
                val isCreator = document.getBoolean("roles.creator") ?: false

                Log.d("UserSettingFragment", "User is admin: $isAdmin, seller: $isSeller, creator: $isCreator")

                buttonAdminPanel.visibility = if (isAdmin) View.VISIBLE else View.GONE
                buttonSellerPanel.visibility = if (isSeller) View.VISIBLE else View.GONE
                buttonCreatorPanel.visibility = if (isCreator) View.VISIBLE else View.GONE
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Failed to check roles: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("UserSettingFragment", "Error checking roles", e)
        }
    }
}
