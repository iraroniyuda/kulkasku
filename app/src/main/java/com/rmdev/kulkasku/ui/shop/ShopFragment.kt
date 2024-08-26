package com.rmdev.kulkasku.ui.shop

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentShopBinding
import com.rmdev.kulkasku.models.Product
import com.rmdev.kulkasku.ui.common.ProductAdapter

class ShopFragment : Fragment() {

    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var productAdapter: ProductAdapter
    private val viewModel: ShopViewModel by viewModels()
    private var sortOption: String = "Newest"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false).apply {
            viewModel = this@ShopFragment.viewModel
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        setupBackButton()
        setupSortSpinner()
        observeViewModel()
        fetchProducts()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_shopFragment_to_homeFragment)
        }
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Newest", "Latest", "Highest Price", "Lowest Price", "Alphabet (A-Z)", "Alphabet (Z-A)")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = adapter
        binding.sortSpinner.setSelection(0) // Set "Newest" as default selection
        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (view != null) {
                    sortOption = sortOptions[position]
                    fetchProducts() // Re-fetch products with the selected sort option
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }
    }

    private fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner, Observer { products ->
            productAdapter.submitList(products)
        })
    }

    private fun fetchProducts() {
        binding.progressBar.visibility = View.VISIBLE // Show loading indicator
        var query: Query = firestore.collection("products")

        when (sortOption) {
            "Highest Price" -> {
                query = query.orderBy("price", Query.Direction.DESCENDING)
            }
            "Lowest Price" -> {
                query = query.orderBy("price", Query.Direction.ASCENDING)
            }
            "Alphabet (A-Z)" -> {
                query = query.orderBy("name", Query.Direction.ASCENDING)
            }
            "Alphabet (Z-A)" -> {
                query = query.orderBy("name", Query.Direction.DESCENDING)
            }
            "Latest" -> {
                query = query.orderBy("createdAt", Query.Direction.ASCENDING)
            }
            "Newest" -> {
                query = query.orderBy("createdAt", Query.Direction.DESCENDING)
            }
        }

        query.get()
            .addOnSuccessListener { result ->
                binding.progressBar.visibility = View.GONE // Hide loading indicator
                val productList = result.toObjects(Product::class.java)
                Log.d("ShopFragment", "Fetched ${productList.size} products")
                viewModel.setProducts(productList)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE // Hide loading indicator
                Log.e("ShopFragment", "Error fetching products", e)
                Toast.makeText(context, "Error fetching products: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
