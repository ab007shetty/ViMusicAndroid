package com.abshetty.vimusic.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    LIBRARY("library", "Library", Icons.Rounded.LibraryMusic),
    SEARCH("search", "Search", Icons.Rounded.Search),
    EQUALIZER("equalizer", "Equalizer", Icons.Rounded.GraphicEq),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings),
}
