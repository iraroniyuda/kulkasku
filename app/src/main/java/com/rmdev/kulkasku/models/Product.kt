package com.rmdev.kulkasku.models

import com.google.firebase.Timestamp

data class Product(
    var id: String = "", // Unique identifier for the product
    val name: String = "", // Name of the product
    val description: String = "", // Description of the product
    val price: Double = 0.0, // Price of the product
    val imageUrl: String = "", // URL of the product image
    val category: String = "", // Category to which the product belongs
    val stock: Int = 0, // Quantity of the product in stock
    val userId: String = "", // ID of the user who owns the product
    val createdAt: Timestamp? = null, // Timestamp of when the product was created
    val updatedAt: Timestamp? = null, // Timestamp of when the product was last updated

    // Additional fields
    val discount: Double = 0.0, // Discount on the product
    val rating: Double = 0.0, // Average rating of the product
    val reviewCount: Int = 0, // Number of reviews for the product
    val isFeatured: Boolean = false, // Whether the product is featured
    val tags: List<String> = listOf(), // Tags associated with the product
    val dimensions: String = "", // Dimensions of the product
    val weight: Double = 0.0, // Weight of the product
    val brand: String = "", // Brand of the product
    val sku: String = "", // Stock Keeping Unit identifier
    val barcode: String = "", // Barcode of the product
    val warranty: String = "", // Warranty information
    val shippingInfo: String = "", // Shipping information
    val returnPolicy: String = "", // Return policy
    val attributes: Map<String, String> = mapOf() // Custom attributes
)
