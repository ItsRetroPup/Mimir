package pup.app.mimir.domain

enum class FrontendLayout {
    FolderAsFile,
    PlaylistAtRootWithoutMoves,
    PlaylistAtRootWithDiscFolder,
}

enum class FrontendPreset(
    val displayName: String,
    val layout: FrontendLayout,
) {
    EsDe("ES-DE", FrontendLayout.FolderAsFile),
    Other("Other", FrontendLayout.PlaylistAtRootWithoutMoves),
}
