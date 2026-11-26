package pup.app.mimir.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview

// Home on a 1080p handheld in landscape, in the longest translations and at a large system font,
// to check that block names fit without splitting words (issue #15).

@Preview(name = "Home · French · 1.3×", locale = "fr", fontScale = 1.3f, widthDp = 840, heightDp = 400)
@Preview(name = "Home · German · 1.3×", locale = "de", fontScale = 1.3f, widthDp = 840, heightDp = 400)
@Preview(name = "Home · Italian · 1.3×", locale = "it", fontScale = 1.3f, widthDp = 840, heightDp = 400)
@Preview(name = "Home · Polish · 1.0×", locale = "pl", widthDp = 840, heightDp = 400)
@Composable
private fun HomeScreenLocalesPreview() {
    MimirTheme(useDarkMode = true) {
        val palette = LocalMimirPalette.current
        Box(Modifier.fillMaxSize().consoleField(palette)) {
            HomeScreen(
                contentFocus = remember { FocusRequester() },
                returnSection = null,
                returnFocus = remember { FocusRequester() },
                onOpen = {},
            )
        }
    }
}

// Home in list view on the same handheld screen, in German for long tool names.

@Preview(name = "Home list · German · 1.3×", locale = "de", fontScale = 1.3f, widthDp = 840, heightDp = 400)
@Preview(name = "Home list · English · phone", widthDp = 400, heightDp = 840)
@Composable
private fun HomeScreenListPreview() {
    MimirTheme(useDarkMode = true) {
        val palette = LocalMimirPalette.current
        Box(Modifier.fillMaxSize().consoleField(palette)) {
            HomeScreen(
                contentFocus = remember { FocusRequester() },
                returnSection = null,
                returnFocus = remember { FocusRequester() },
                onOpen = {},
                listView = true,
            )
        }
    }
}
