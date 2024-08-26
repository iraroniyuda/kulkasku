package com.rmdev.kulkasku.ui.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentProductDetailBinding
import com.rmdev.kulkasku.models.Product
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.text.NumberFormat
import java.util.Locale

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val args: ProductDetailFragmentArgs by navArgs()
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        fetchProductDetails(args.productId)

        binding.addToCartButton.setOnClickListener {
            addToCart(args.productId)
        }

        binding.continueShoppingButton.setOnClickListener {
            findNavController().navigate(R.id.action_productDetailFragment_to_navigation_shop)
        }
    }

    private fun fetchProductDetails(productId: String) {
        firestore.collection("products").document(productId).get()
            .addOnSuccessListener { document ->
                val product = document.toObject(Product::class.java)
                product?.let {
                    bindProductDetails(it)
                }
            }
            .addOnFailureListener { e ->
                // Handle the error
            }
    }

    private fun bindProductDetails(product: Product) {
        binding.apply {
            productName.text = product.name
            productDescription.text = product.description
            productPrice.text = formatPriceToRupiah(product.price)
            Glide.with(this@ProductDetailFragment)
                .load(product.imageUrl)
                .transform(RoundedCornersTransformation(64, 0))
                .into(productImage)
            // Bind other product details
        }
    }

    private fun formatPriceToRupiah(price: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return format.format(price)
    }

    private fun addToCart(productId: String) {
        // Logic to add the product to the cart
        // This can involve saving the product to a local database or shared preferences
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
