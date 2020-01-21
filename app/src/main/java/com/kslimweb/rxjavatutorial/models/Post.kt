package com.kslimweb.rxjavatutorial.models

data class Post(
    val body: String,
    val id: Int,
    val title: String,
    val userId: Int,
    var comments: List<Comment>
)