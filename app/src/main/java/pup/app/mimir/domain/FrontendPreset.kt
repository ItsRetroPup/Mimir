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
    Cocoon("Cocoon", FrontendLayout.PlaylistAtRootWithoutMoves),
    Iisu("iiSU", FrontendLayout.PlaylistAtRootWithoutMoves),
    Beacon("Beacon", FrontendLayout.PlaylistAtRootWithoutMoves),
    ConsoleLauncher("Console Launcher", FrontendLayout.PlaylistAtRootWithoutMoves),
    NeoStation("NeoStation", FrontendLayout.PlaylistAtRootWithoutMoves),
    Daijisho("Daijisho", FrontendLayout.PlaylistAtRootWithoutMoves),
    Retrohrai("Retrohrai", FrontendLayout.PlaylistAtRootWithoutMoves),
}
