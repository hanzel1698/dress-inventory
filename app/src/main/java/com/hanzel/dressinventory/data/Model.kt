package com.hanzel.dressinventory.data

import kotlinx.serialization.Serializable

@Serializable
enum class Category { TOP, BOTTOM }

@Serializable
enum class Pattern { SOLID, PATTERNED }

@Serializable
data class Dress(
    val id: String,
    val name: String,
    val category: Category,
    val type: String,
    val colorName: String,
    val colorHex: Long,
    val pattern: Pattern = Pattern.SOLID,
    val photoPath: String? = null,
)

/** wearLog: ISO date (yyyy-MM-dd) -> ids of dresses worn that day */
@Serializable
data class AppData(
    val dresses: List<Dress> = emptyList(),
    val wearLog: Map<String, List<String>> = emptyMap(),
    val shoppingWishlist: List<String> = emptyList(),
)

data class ChartColor(val name: String, val hex: Long, val neutral: Boolean = false)

/** Curated chart of natural-light friendly colours. */
val COLOR_CHART = listOf(
    ChartColor("White", 0xFFFAFAF7, neutral = true),
    ChartColor("Cream", 0xFFF6EFD9, neutral = true),
    ChartColor("Beige", 0xFFD9C5A0, neutral = true),
    ChartColor("Tan", 0xFFC19A6B, neutral = true),
    ChartColor("Khaki", 0xFFB9A37E, neutral = true),
    ChartColor("Light Grey", 0xFFC9CDD2, neutral = true),
    ChartColor("Grey", 0xFF8E9299, neutral = true),
    ChartColor("Charcoal", 0xFF44474F, neutral = true),
    ChartColor("Black", 0xFF1C1C1E, neutral = true),
    ChartColor("Brown", 0xFF6F4E37, neutral = true),
    ChartColor("Navy", 0xFF232F4B, neutral = true),
    ChartColor("Denim Blue", 0xFF3F5E8C, neutral = true),
    ChartColor("Light Denim", 0xFF7E9CC0, neutral = true),
    ChartColor("Sky Blue", 0xFFA7C7E7),
    ChartColor("Royal Blue", 0xFF2B4FA2),
    ChartColor("Teal", 0xFF2E7E85),
    ChartColor("Mint", 0xFFA8D5BA),
    ChartColor("Olive", 0xFF6B7239, neutral = true),
    ChartColor("Forest Green", 0xFF2F5D3A),
    ChartColor("Mustard", 0xFFD4A537),
    ChartColor("Yellow", 0xFFF2D14E),
    ChartColor("Peach", 0xFFF4B58F),
    ChartColor("Orange", 0xFFE07A2F),
    ChartColor("Rust", 0xFFB5532A),
    ChartColor("Coral", 0xFFEE6F57),
    ChartColor("Red", 0xFFC03A2B),
    ChartColor("Maroon", 0xFF6E1F2C),
    ChartColor("Pink", 0xFFE8A0B4),
    ChartColor("Hot Pink", 0xFFD44480),
    ChartColor("Lavender", 0xFFB9A7D6),
    ChartColor("Purple", 0xFF6A4C93),
)

val TOP_TYPES = listOf("T-Shirt", "Shirt", "Polo", "Kurta", "Sweater", "Hoodie", "Jacket")
val BOTTOM_TYPES = listOf("Jeans", "Pants", "Trousers", "Shorts", "Joggers", "Track Pants")
