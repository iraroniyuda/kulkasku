package com.rmdev.kulkasku.ui.common

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rmdev.kulkasku.R
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import com.rmdev.kulkasku.databinding.ItemProductBinding
import com.rmdev.kulkasku.models.Product
import com.rmdev.kulkasku.ui.home.HomeFragmentDirections
import com.rmdev.kulkasku.ui.shop.ShopFragmentDirections
import com.rmdev.kulkasku.ui.shop.SayurFragmentDirections
import com.rmdev.kulkasku.ui.shop.BuahFragmentDirections
import com.rmdev.kulkasku.ui.shop.SembakoFragmentDirections
import com.rmdev.kulkasku.ui.shop.FrozenFragmentDirections
import com.rmdev.kulkasku.ui.shop.ProteinFragmentDirections
import com.rmdev.kulkasku.ui.shop.BumbuFragmentDirections
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import androidx.navigation.findNavController


class ProductAdapter(
    private val onUpdateClick: ((Product) -> Unit)? = null,
    private val onDeleteClick: ((Product) -> Unit)? = null
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var products: List<Product> = emptyList()

    fun submitList(productList: List<Product>) {
        products = productList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], onUpdateClick, onDeleteClick)
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product, onUpdateClick: ((Product) -> Unit)?, onDeleteClick: ((Product) -> Unit)?) {
            binding.apply {
                productName.text = product.name

                // If there is a discount, display the original price with strikethrough and the discounted price with discount percentage
                if (product.discount > 0) {
                    val discountPercentage = ceil((product.discount / product.price) * 100).toInt()

                    productPrice.text = formatPriceToRupiah(product.price)
                    productPrice.paintFlags = productPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    productPrice.setTextColor(root.context.getColor(android.R.color.darker_gray))

                    productDiscountedPrice.visibility = View.VISIBLE
                    productDiscountedPrice.text = formatPriceToRupiah(product.discount)

                    productDiscountPercentage.visibility = View.VISIBLE
                    productDiscountPercentage.text = "(${discountPercentage}% OFF)"

                    // Make the discount layout visible
                    discountLayout.visibility = View.VISIBLE
                } else {
                    productPrice.text = formatPriceToRupiah(product.price)
                    productPrice.paintFlags = productPrice.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    productPrice.setTextColor(root.context.getColor(android.R.color.black))

                    productDiscountedPrice.visibility = View.GONE
                    productDiscountPercentage.visibility = View.GONE

                    // Make the discount layout gone
                    discountLayout.visibility = View.GONE
                }

                // Load image with rounded corners transformation
                Glide.with(productImage.context)
                    .load(product.imageUrl)
                    .transform(RoundedCornersTransformation(64, 0))
                    .into(productImage)

                if (onUpdateClick != null && onDeleteClick != null) {
                    updateButton.visibility = View.VISIBLE
                    deleteButton.visibility = View.VISIBLE
                    updateButton.setOnClickListener { onUpdateClick(product) }
                    deleteButton.setOnClickListener { onDeleteClick(product) }
                } else {
                    updateButton.visibility = View.GONE
                    deleteButton.visibility = View.GONE
                }

                // Add click listener for navigation to product detail page
                root.setOnClickListener {
                    val action = when (it.findNavController().currentDestination?.id) {
                        R.id.navigation_home -> HomeFragmentDirections.actionHomeFragmentToProductDetailFragment(product.id)
                        R.id.navigation_shop -> ShopFragmentDirections.actionShopFragmentToProductDetailFragment(product.id)
                        R.id.navigation_sayur -> SayurFragmentDirections.actionSayurFragmentToProductDetailFragment(product.id)
                        R.id.navigation_buah -> BuahFragmentDirections.actionBuahFragmentToProductDetailFragment(product.id)
                        R.id.navigation_sembako -> SembakoFragmentDirections.actionSembakoFragmentToProductDetailFragment(product.id)
                        R.id.navigation_frozen -> FrozenFragmentDirections.actionFrozenFragmentToProductDetailFragment(product.id)
                        R.id.navigation_protein -> ProteinFragmentDirections.actionProteinFragmentToProductDetailFragment(product.id)
                        R.id.navigation_bumbu -> BumbuFragmentDirections.actionBumbuFragmentToProductDetailFragment(product.id)
                        else -> null
                    }
                    action?.let { it1 -> it.findNavController().navigate(it1) }
                }
            }
        }

        private fun formatPriceToRupiah(price: Double): String {
            val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            return format.format(price)
        }
    }
}
