package com.rmdev.kulkasku.ui.shop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rmdev.kulkasku.models.Product

class ShopViewModel : ViewModel() {
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> get() = _products

    fun setProducts(productList: List<Product>) {
        _products.value = productList
    }
}
