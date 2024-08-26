package com.rmdev.kulkasku.ui.home

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.rmdev.kulkasku.R
import com.rmdev.kulkasku.databinding.FragmentHomeBinding
import com.rmdev.kulkasku.models.Product
import com.rmdev.kulkasku.ui.common.ProductAdapter
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var carouselAdapter: CarouselAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var productAdapter: ProductAdapter
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val pageChangeDelay: Long = 6000 // 6 seconds

    private val changePageRunnable = object : Runnable {
        override fun run() {
            val currentItem: Int = binding.carousel.currentItem
            val nextItem: Int = (currentItem + 1) % carouselAdapter.count
            binding.carousel.setCurrentItem(nextItem, true)
            handler.postDelayed(this, pageChangeDelay)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        _binding = FragmentHomeBinding.inflate(inflater, container, false).apply {
            viewModel = homeViewModel
            lifecycleOwner = viewLifecycleOwner
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageList: List<Int> = listOf(R.drawable.image1, R.drawable.image2, R.drawable.image3) // Add your images here
        carouselAdapter = CarouselAdapter(requireContext(), imageList)
        binding.carousel.adapter = carouselAdapter

        // Start automatic cycling when the view is created
        handler.postDelayed(changePageRunnable, pageChangeDelay)

        setupRecyclerView()
        fetchProducts()
        setupSearchButton()
        setupDimOverlay()

        // Setup profile icon click listener
        binding.profileIcon.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        setupImageButtons()
        setupAllItemButton()
        setupSayurButton()
        setupBuahButton()
        setupSembakoButton()
        setupFrozenButton()
        setupProteinButton()
        setupBumbuButton()
        setupLainnyaButton()
    }

    private fun setupSearchButton() {
        binding.searchIcon.setOnClickListener {
            showFloatingSearchBar()
        }
    }

    private fun setupDimOverlay() {
        binding.dimOverlay.setOnClickListener {
            hideFloatingSearchBar()
        }
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
        }
    }

    private fun showFloatingSearchBar() {
        binding.floatingSearchBar.root.visibility = View.VISIBLE
        binding.dimOverlay.visibility = View.VISIBLE
        binding.dimOverlay.isClickable = true
    }

    private fun hideFloatingSearchBar() {
        binding.floatingSearchBar.root.visibility = View.GONE
        binding.dimOverlay.visibility = View.GONE
        binding.dimOverlay.isClickable = false
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun fetchProducts() {
        firestore = FirebaseFirestore.getInstance()
        firestore.collection("products").get()
            .addOnSuccessListener { result ->
                val productList = result.toObjects(Product::class.java)
                productAdapter.submitList(productList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error fetching products: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupImageButtons() {
        Glide.with(this)
            .load(R.drawable.ic_all_item)
            .transform(CircleCrop())
            .into(binding.allitem)

        Glide.with(this)
            .load(R.drawable.ic_sayur)
            .transform(CircleCrop())
            .into(binding.icSayur)

        Glide.with(this)
            .load(R.drawable.ic_sembako)
            .transform(CircleCrop())
            .into(binding.icSembako)

        Glide.with(this)
            .load(R.drawable.ic_buah)
            .transform(CircleCrop())
            .into(binding.icBuah)

        Glide.with(this)
            .load(R.drawable.ic_frozen)
            .transform(CircleCrop())
            .into(binding.icFrozen)

        Glide.with(this)
            .load(R.drawable.ic_bumbu)
            .transform(CircleCrop())
            .into(binding.icBumbu)

        Glide.with(this)
            .load(R.drawable.ic_protein)
            .transform(CircleCrop())
            .into(binding.icProtein)

        Glide.with(this)
            .load(R.drawable.ic_lainnya)
            .transform(CircleCrop())
            .into(binding.icLainnya)
    }

    private fun setupAllItemButton() {
        binding.allitem.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_shopFragment)
        }
    }

    private fun setupSayurButton() {
        binding.icSayur.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_sayurFragment)
        }
    }

    private fun setupBuahButton() {
        binding.icBuah.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_buahFragment)
        }
    }

    private fun setupSembakoButton() {
        binding.icSembako.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_sembakoFragment)
        }
    }

    private fun setupFrozenButton() {
        binding.icFrozen.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_frozenFragment)
        }
    }

    private fun setupProteinButton() {
        binding.icProtein.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_proteinFragment)
        }
    }

    private fun setupBumbuButton() {
        binding.icBumbu.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_bumbuFragment)
        }
    }

    private fun setupLainnyaButton() {
        binding.icLainnya.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_moreFragment)
        }
    }


    override fun onResume() {
        super.onResume()
        handler.postDelayed(changePageRunnable, pageChangeDelay)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(changePageRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(changePageRunnable)
        _binding = null
    }
}