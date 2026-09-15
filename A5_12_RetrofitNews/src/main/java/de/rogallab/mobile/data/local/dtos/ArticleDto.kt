package de.rogallab.mobile.data.local.dtos

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "Article")
data class ArticleDto(
   @PrimaryKey val url: String,
   val sourceName: String,
   val author: String?,
   val title: String,
   val description: String?,
   val content: String?,
   val publishedAt: String,
   val imageUrl: String?,
)
