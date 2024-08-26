package com.rmdev.kulkasku.ui.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rmdev.kulkasku.models.Product

class CartViewModel : ViewModel() {

    private val _cartProducts = MutableLiveData<List<Product>>()
    val cartProducts: LiveData<List<Product>> get() = _cartProducts

    fun addProductToCart(product: Product) {
        val currentProducts = _cartProducts.value ?: emptyList()
        _cartProducts.value = currentProducts + product
    }
}
