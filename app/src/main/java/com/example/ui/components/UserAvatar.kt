package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage

/**
 * Profile pictures of every trader we know about, keyed by handle (e.g. "@john").
 * Filled in by the ViewModel; any screen can read it and it redraws when a photo arrives.
 */
object AvatarStore {
    val photos = mutableStateMapOf<String, String>()

    private fun key(handle: String) = handle.trim().removePrefix("@").lowercase()

    fun put(handle: String, uri: String) {
        if (handle.isNotBlank()) photos[key(handle)] = uri
    }

    fun get(handle: String?): String? = handle?.takeIf { it.isNotBlank() }?.let { photos[key(it)] }
}

/** Fills its parent (a circular Box): the trader's photo if we have one, otherwise their initials. */
@Composable
fun AvatarContent(
    handle: String?,
    initials: String,
    textStyle: TextStyle,
    textColor: Color
) {
    val photo = AvatarStore.get(handle)
    if (photo != null) {
        AsyncImage(
            model = photo,
            contentDescription = "Profile picture",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Text(text = initials, style = textStyle, color = textColor)
    }
}
