package de.rogallab.mobile.data.dtos

import androidx.room.*

@Entity(tableName = "Article")
data class Article(
   @PrimaryKey(autoGenerate = true)
   val id: Int? = null,
   val author: String = "",
   val content: String? = "",
   val description: String? = "",
   val publishedAt: String = "",
   val source: Source?,
   val title: String = "",
   val url: String = "",
   val urlToImage: String? = ""
)