package com.rmdev.kulkasku.ui.seller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.rmdev.kulkasku.databinding.ActivityAddProductBinding
import com.rmdev.kulkasku.models.Product
import java.text.NumberFormat
import java.util.*

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize category spinner with a hint
        val categories = listOf("Choose Category", "Sayur", "Sembako", "Buah", "Frozen Food", "Bumbu", "Protein", "Lain-lain")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.productCategorySpinner.adapter = adapter
        binding.productCategorySpinner.setSelection(0) // Set default selection to the hint

        binding.productCategorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position == 0) {
                    // Do nothing, or show a message
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Add TextWatcher to format price and discount inputs
        binding.productPrice.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    binding.productPrice.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[,]".toRegex(), "")
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0
                    val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)

                    current = formatted
                    binding.productPrice.setText(formatted)
                    binding.productPrice.setSelection(formatted.length)

                    binding.productPrice.addTextChangedListener(this)
                }
            }
        })

        binding.productDiscount.addTextChangedListener(object : TextWatcher {
            private var current = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                if (s.toString() != current) {
                    binding.productDiscount.removeTextChangedListener(this)

                    val cleanString = s.toString().replace("[,]".toRegex(), "")
                    val parsed = cleanString.toDoubleOrNull() ?: 0.0
                    val formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed)

                    current = formatted
                    binding.productDiscount.setText(formatted)
                    binding.productDiscount.setSelection(formatted.length)

                    binding.productDiscount.addTextChangedListener(this)
                }
            }
        })

        binding.productImageUrl.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        binding.saveButton.setOnClickListener {
            val name = binding.productName.text.toString()
            val description = binding.productDescription.text.toString()
            val priceString = binding.productPrice.text.toString().replace("[,]".toRegex(), "")
            val price = priceString.toDoubleOrNull()
            val category = binding.productCategorySpinner.selectedItem.toString()
            if (category == "Choose Category") {
                Toast.makeText(this, "Please choose a valid category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (price == null || price <= 0) {
                Toast.makeText(this, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val stock = binding.productStock.text.toString().toIntOrNull() ?: 0
            val discountString = binding.productDiscount.text.toString().replace("[,]".toRegex(), "")
            val discount = discountString.toDoubleOrNull() ?: 0.0
            if (discount > price) {
                Toast.makeText(this, "Discount price cannot be higher than the original price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val tags = binding.productTags.text.toString().split(",").map { it.trim() }
            val weight = binding.productWeight.text.toString().toDoubleOrNull() ?: 0.0
            val brand = binding.productBrand.text.toString()
            val timestamp = Timestamp(Date())
            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            if (imageUri != null) {
                uploadImageToFirebaseStorage(imageUri!!) { imageUrl ->
                    val product = Product(
                        name = name,
                        description = description,
                        price = price,
                        imageUrl = imageUrl,
                        category = category,
                        stock = stock,
                        userId = userId,
                        createdAt = timestamp,
                        updatedAt = timestamp,
                        discount = discount,
                        tags = tags,
                        weight = weight,
                        brand = brand
                    )

                    firestore.collection("products").add(product)
                        .addOnSuccessListener { documentReference ->
                            product.id = documentReference.id // Set the generated document id
                            documentReference.set(product) // Update the document with the id
                            Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK) // Set result to OK
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add product: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelButton.setOnClickListener {
            finish()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            imageUri = result.data!!.data
            binding.productImageUrl.setText(imageUri.toString())
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri, callback: (String) -> Unit) {
        val storageRef = storage.reference.child("product_images/${UUID.randomUUID()}.jpg")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                callback(uri.toString())
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get image URL: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
