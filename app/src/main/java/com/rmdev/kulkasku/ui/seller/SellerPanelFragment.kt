package com.rmdev.kulkasku.ui.seller

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentSellerPanelBinding
import com.rmdev.kulkasku.models.Product
import com.rmdev.kulkasku.ui.common.ProductAdapter
import java.text.NumberFormat
import java.util.*

class SellerPanelFragment : Fragment() {

    private var _binding: FragmentSellerPanelBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var productAdapter: ProductAdapter

    private val addProductLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            fetchProducts() // Refresh the products list
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerPanelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        setupRecyclerView()
        fetchProducts()

        binding.addProductButton.setOnClickListener {
            val intent = Intent(requireContext(), AddProductActivity::class.java)
            addProductLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(
            onUpdateClick = { product -> showUpdateDialog(product) },
            onDeleteClick = { product -> deleteProduct(product) }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun fetchProducts() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("products")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val productList = result.toObjects(Product::class.java)
                productAdapter.submitList(productList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to fetch products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUpdateDialog(product: Product) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_product, null)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.updateProductDescription)
        val priceEditText = dialogView.findViewById<EditText>(R.id.updateProductPrice)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.updateProductCategory)
        val stockEditText = dialogView.findViewById<EditText>(R.id.updateProductStock)
        val discountEditText = dialogView.findViewById<EditText>(R.id.updateProductDiscount)
        val tagsEditText = dialogView.findViewById<EditText>(R.id.updateProductTags)
        val weightEditText = dialogView.findViewById<EditText>(R.id.updateProductWeight)
        val brandEditText = dialogView.findViewById<EditText>(R.id.updateProductBrand)

        descriptionEditText.setText(product.description)
        priceEditText.setText(NumberFormat.getNumberInstance(Locale.US).format(product.price))
        stockEditText.setText(product.stock.toString())
        discountEditText.setText(NumberFormat.getNumberInstance(Locale.US).format(product.discount))
        tagsEditText.setText(product.tags.joinToString(", "))
        weightEditText.setText(NumberFormat.getNumberInstance(Locale.US).format(product.weight))
        brandEditText.setText(product.brand)

        // Initialize category spinner
        val categories = listOf("Choose Category", "Sayur", "Sembako", "Buah", "Frozen Food", "Bumbu", "Protein", "Lain-lain")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
        categorySpinner.setSelection(categories.indexOf(product.category))

        // Add TextWatcher to format price and discount inputs
        priceEditText.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    priceEditText.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[,]".toRegex(), "")
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0
                    val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)

                    current = formatted
                    priceEditText.setText(formatted)
                    priceEditText.setSelection(formatted.length)

                    priceEditText.addTextChangedListener(this)
                }
            }
        })

        discountEditText.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    discountEditText.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[,]".toRegex(), "")
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0
                    val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)

                    current = formatted
                    discountEditText.setText(formatted)
                    discountEditText.setSelection(formatted.length)

                    discountEditText.addTextChangedListener(this)
                }
            }
        })

        val dialog = AlertDialog.Builder(context)
            .setTitle("Update Product")
            .setView(dialogView)
            .setPositiveButton("Update", null) // We will override this later to keep the dialog open
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val updateButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            updateButton.setOnClickListener {
                val updatedCategory = categorySpinner.selectedItem.toString()
                if (updatedCategory == "Choose Category") {
                    Toast.makeText(context, "Please choose a valid category", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val updatedPriceString = priceEditText.text.toString().replace("[,]".toRegex(), "")
                val updatedPrice = updatedPriceString.toDoubleOrNull()
                if (updatedPrice == null || updatedPrice <= 0) {
                    Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val updatedDiscountString = discountEditText.text.toString().replace("[,]".toRegex(), "")
                val updatedDiscount = updatedDiscountString.toDoubleOrNull() ?: 0.0
                if (updatedDiscount > updatedPrice) {
                    Toast.makeText(context, "Discount price cannot be higher than the original price", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val updatedProduct = product.copy(
                    description = descriptionEditText.text.toString(),
                    price = updatedPrice,
                    category = updatedCategory,
                    stock = stockEditText.text.toString().toIntOrNull() ?: 0,
                    discount = updatedDiscount,
                    tags = tagsEditText.text.toString().split(",").map { it.trim() },
                    weight = weightEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    brand = brandEditText.text.toString()
                )
                updateProduct(updatedProduct)
                dialog.dismiss() // Close the dialog only if the update is successful
            }
        }

        dialog.show()
    }

    private fun updateProduct(product: Product) {
        firestore.collection("products").document(product.id).set(product)
            .addOnSuccessListener {
                Toast.makeText(context, "Product updated successfully", Toast.LENGTH_SHORT).show()
                fetchProducts() // Refresh the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteProduct(product: Product) {
        AlertDialog.Builder(context)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product?")
            .setPositiveButton("Yes") { _, _ ->
                // Delete the image from Firebase Storage first
                val imageRef = storage.getReferenceFromUrl(product.imageUrl)
                imageRef.delete().addOnSuccessListener {
                    // Image deleted successfully, now delete the product document
                    firestore.collection("products").document(product.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                            fetchProducts() // Refresh the list
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to delete product: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to delete product image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
