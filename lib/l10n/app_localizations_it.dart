// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Italian (`it`).
class AppLocalizationsIt extends AppLocalizations {
  AppLocalizationsIt([String locale = 'it']) : super(locale);

  @override
  String get appName => 'Mimir';

  @override
  String get home => 'Home';

  @override
  String get goHome => 'Vai alla Home';

  @override
  String get toggleDarkMode => 'Attiva o disattiva la modalità scura';

  @override
  String get status => 'Stato';

  @override
  String get homeIntro => 'Benvenuto in Mimir!';

  @override
  String get homePrompt =>
      'Scegli uno strumento dall’elenco qui sotto per iniziare';

  @override
  String get homePrepareLibrary => 'Strumenti utili';

  @override
  String get homeMakeRoom => 'Risparmia spazio sul tuo dispositivo';

  @override
  String get supportMimir => 'Support Mimir';

  @override
  String get supportCopy =>
      'Mimir is, and always will be, 100% free and without ads. If you would like to show your support, please consider checking out my YouTube channel or donating.';

  @override
  String get youtube => 'YouTube';

  @override
  String get kofi => 'Ko-fi';

  @override
  String get buyMeACoffee => 'Buy Me a Coffee';

  @override
  String get goalFixMultidisc => 'Correggi i giochi multidisco';

  @override
  String get toolMultidisc => 'Organizzatore multidisco';

  @override
  String get toolMultidiscDescription =>
      'Crea cartelle e playlist pronte per il frontend.';

  @override
  String get goalCreateVitaShortcuts => 'Crea collegamenti Vita';

  @override
  String get toolVitaShortcuts => 'Collegamenti Vita';

  @override
  String get toolVitaShortcutsDescription =>
      'Crea collegamenti .psvita o .dpt dal database integrato.';

  @override
  String get goalCreateScummVmLaunchers => 'Create ScummVM launchers';

  @override
  String get toolScummVmLaunchers => 'ScummVM Launchers';

  @override
  String get toolScummVmLaunchersDescription =>
      'Detect games and create .scummvm files beside them.';

  @override
  String get goalSetupEsde => 'Configura ES-DE';

  @override
  String get toolEsdeSystems => 'Sistemi ES-DE';

  @override
  String get toolEsdeSystemsDescription =>
      'Installa sistemi personalizzati e associa le relative cartelle ROM.';

  @override
  String get goalConvertSwitchPackages => 'Converti pacchetti Switch';

  @override
  String get toolNszToNsp => 'Da NSZ a NSP';

  @override
  String get toolNszToNspDescription => 'Decomprimi file NSZ in pacchetti NSP.';

  @override
  String get goalCompressCartridgeRoms => 'Comprimi ROM su cartuccia';

  @override
  String get toolRomzipper => 'RomZipper';

  @override
  String get toolRomzipperDescription =>
      'Archivia le ROM su cartuccia supportate come file .zip.';

  @override
  String get goalCompressDiscImages => 'Comprimi immagini disco';

  @override
  String get toolChdman => 'CHDMan';

  @override
  String get toolChdmanDescription =>
      'Converti immagini PSX, PS2, PSP, Saturn, Dreamcast e Sega CD in CHD.';

  @override
  String get goalShrinkGamecubeWii => 'Riduci giochi GameCube e Wii';

  @override
  String get toolDolphinRvz => 'Dolphin RVZ';

  @override
  String get toolDolphinRvzDescription => 'Converti immagini ISO in RVZ.';

  @override
  String get goalShrink3ds => 'Riduci giochi Nintendo 3DS';

  @override
  String get toolAzaharZcci => 'Azahar ZCCI';

  @override
  String get toolAzaharZcciDescription =>
      'Comprimi immagini 3DS e CCI decrittografate in ZCCI.';

  @override
  String toolCardDescription(Object tool, Object description) {
    return '$tool • $description';
  }

  @override
  String get noRomFolderSelected => 'Nessuna cartella ROM selezionata';

  @override
  String get noVitaOutputSelected =>
      'Nessuna cartella di output Vita selezionata';

  @override
  String get noScummVmFolderSelected => 'No ScummVM Games folder selected';

  @override
  String get noEsdeFolderSelected => 'Nessuna cartella ES-DE selezionata';

  @override
  String get selectedRomFolder => 'Selected ROM folder';

  @override
  String get selectRomFolder => 'Select ROM folder';

  @override
  String get scummVmGamesFolder => 'ScummVM Games folder';

  @override
  String get selectScummVmGamesFolder => 'Select Games folder';

  @override
  String get scummVmExecutable => 'ScummVM executable';

  @override
  String get scummVmExecutableAuto =>
      'Auto-detected on scan; choose an app if needed.';

  @override
  String get selectScummVmExecutable =>
      'Choose the ScummVM app to run detection.';

  @override
  String get scummVmDescription =>
      'Run --detect for each game folder and create a launcher file containing its game ID.';

  @override
  String get shortcutOutputDirectory => 'Shortcut output directory';

  @override
  String get selectOutputFolder => 'Select output folder';

  @override
  String get shortcutFileType => 'Shortcut file type';

  @override
  String get cocoonHint => 'For Cocoon users, select .dpt';

  @override
  String get shortcutDatabase => 'Shortcut database';

  @override
  String titlesReady(int count) {
    return '$count titles ready for search';
  }

  @override
  String get searchTitleOrAppId => 'Search title or app ID';

  @override
  String get searchResults => 'Search results';

  @override
  String get quickAdd => 'Quick add';

  @override
  String deleteShortcut(Object title) {
    return 'Delete shortcut for $title';
  }

  @override
  String get selectedEsdeFolder => 'Selected ES-DE folder';

  @override
  String get selectEsdeFolder => 'Select ES-DE folder';

  @override
  String get romRootFolder => 'ROM root folder';

  @override
  String get selectRomRoot => 'Select ROM root';

  @override
  String get downloadLatestXmls => 'Download latest XMLs';

  @override
  String get installCustomSystems => 'Install custom systems';

  @override
  String get chooseFolder => 'Choose folder';

  @override
  String get defaultLabel => 'Default';

  @override
  String get esdeEmpty =>
      'Download the latest XMLs to configure the custom Android systems. They will be installed under custom_systems.';

  @override
  String get esdeFolderDescription =>
      'Install the latest ES-DE Android custom systems into custom_systems.';

  @override
  String get organizerDescription =>
      'Organise multi-disc games into frontend-ready folders and playlists.';

  @override
  String get zipperDescription =>
      'Compress supported ROM files into .zip archives to save space.';

  @override
  String get vitaDescription =>
      'Create .psvita or .dpt shortcut files from the built-in Vita database.';

  @override
  String get frontEndTarget => 'Frontend target';

  @override
  String get otherFrontend => 'Other';

  @override
  String get scanHiddenFolders => 'Scan hidden folders';

  @override
  String get scanHiddenDescription =>
      'Extract ROMs from .-prefixed folders into the system folder before organizing.';

  @override
  String get nszKeys => 'NSZ keys';

  @override
  String get nszKeysConfigured =>
      'prod.keys is imported and will be kept in Mimir\'s private storage.';

  @override
  String get nszKeysMissing =>
      'Import your legally obtained prod.keys file before scanning or converting NSZ packages.';

  @override
  String get importProdKeys => 'Import prod.keys';

  @override
  String get replaceProdKeys => 'Replace prod.keys';

  @override
  String get system => 'System';

  @override
  String get conversionType => 'Conversion type';

  @override
  String get compatibilityGuidance => 'Compatibility guidance';

  @override
  String get ps2Guidance =>
      'Use CD when using NetherSX2; use DVD when using ARMSX2.';

  @override
  String get preview => 'Preview';

  @override
  String previewSummary(int count, int selected) {
    return '$count multi-disc sets detected; $selected selected.';
  }

  @override
  String zipPreviewSummary(int count, int selected) {
    return '$count ROMs matched the zip whitelist; $selected selected.';
  }

  @override
  String jobsSelected(int count, Object size) {
    return '$count jobs selected • $size';
  }

  @override
  String get scanSelectedFolder => 'Scan selected folder';

  @override
  String applySelected(int count) {
    return 'Apply $count selected';
  }

  @override
  String convertSelected(int count) {
    return 'Convert $count selected';
  }

  @override
  String get applyChanges => 'Apply changes';

  @override
  String get cancel => 'Cancel';

  @override
  String get selectAll => 'Select all';

  @override
  String get deselectAll => 'Deselect all';

  @override
  String get conversionQueue => 'Conversion queue';

  @override
  String get sortBy => 'Sort by';

  @override
  String get storageOnDevice => 'Storage on scanned device';

  @override
  String get romHeader => 'ROM';

  @override
  String get sizeHeader => 'SIZE';

  @override
  String get outputHeader => 'OUTPUT';

  @override
  String get existingOutput =>
      'Existing output — select individually to replace';

  @override
  String get replaceExistingOutput => 'Replace existing output?';

  @override
  String replaceOutputBody(Object path) {
    return '$path already exists and will be overwritten if selected.';
  }

  @override
  String get replaceOutput => 'Replace output';

  @override
  String get sourceLabel => 'Sources';

  @override
  String get targetLabel => 'Targets';

  @override
  String get noChanges => 'No changes found.';

  @override
  String get selectFolderFirst => 'Select a ROM folder first.';

  @override
  String get importKeysFirst =>
      'Import a prod.keys file before scanning NSZ packages.';

  @override
  String get changesApplied => 'Changes applied.';

  @override
  String get operationFailed => 'Operation failed.';

  @override
  String get conversionReport => 'Conversion report';

  @override
  String get supportDescription =>
      'Mimir is, and always will be, 100% free and without ads. If you would like to show your support, please consider checking out my YouTube channel or donating.';

  @override
  String get zipWhitelistInfo =>
      'Supported extensions are selected by the current whitelist. Existing .zip outputs are skipped safely.';

  @override
  String get discType => 'Disc type';

  @override
  String get deleteOriginalFiles =>
      'Delete original files after a successful conversion';

  @override
  String conflictsSkipped(int count) {
    return '$count conflicting outputs were skipped.';
  }

  @override
  String get applyConfirmationBody =>
      'Mimir will execute the selected changes sequentially. Partial completion will be reported if you stop or an operation fails.';

  @override
  String get working => 'Working…';

  @override
  String get stopAfterCurrent => 'Stop after current';

  @override
  String get stopNow => 'Stop now';

  @override
  String get noMatchingTitles => 'No matching titles.';

  @override
  String systemsCount(int count) {
    return '$count systems';
  }

  @override
  String get prodKeysImported => 'prod.keys imported and saved on this device.';

  @override
  String get importKeysFailed => 'Unable to import prod.keys.';

  @override
  String get selectRomFolderFirst => 'Select a ROM folder first.';

  @override
  String get selectScummVmFolderFirst => 'Select a ScummVM Games folder first.';

  @override
  String get scummVmExecutableNotFound =>
      'ScummVM was not found. Choose the app file to continue.';

  @override
  String get scummVmDesktopOnly =>
      'ScummVM executable detection is currently available on desktop platforms only.';

  @override
  String get toolDoesNotUseRomScanning =>
      'This tool does not use ROM scanning.';

  @override
  String get scanStopped => 'Scan stopped.';

  @override
  String get scanFailed => 'Scan failed.';

  @override
  String get operationStopped => 'Operation stopped.';

  @override
  String stoppedAfterOperations(int completed, int total) {
    return 'Stopped after $completed of $total operations.';
  }

  @override
  String get applyFailed => 'Apply failed.';

  @override
  String get stopping => 'Stopping…';

  @override
  String get stopAfterCurrentMessage =>
      'Will stop after the current conversion.';

  @override
  String get selectVitaOutputFirst =>
      'Select a Vita shortcut output folder first.';

  @override
  String unableToAddShortcut(Object title) {
    return 'Unable to add shortcut for $title.';
  }

  @override
  String addedShortcut(Object title) {
    return 'Added shortcut: $title';
  }

  @override
  String get addShortcutFailed => 'Add shortcut failed.';

  @override
  String removedShortcut(Object title) {
    return 'Removed shortcut: $title';
  }

  @override
  String get removeShortcutFailed => 'Remove shortcut failed.';

  @override
  String get selectEsdeFolderFirst => 'Select the ES-DE folder first.';

  @override
  String get selectRomRootForEsde =>
      'Select the ROM root folder first so Mimir can find its systems.';

  @override
  String get downloadingEsde => 'Downloading the latest ES-DE custom systems…';

  @override
  String downloadedEsde(int count) {
    return 'Downloaded $count ES-DE systems. Review the folders, then apply them.';
  }

  @override
  String get downloadEsdeFailed => 'Unable to download ES-DE systems.';

  @override
  String get installingEsde => 'Installing ES-DE custom systems…';

  @override
  String installedEsde(int count) {
    return 'Installed $count systems in custom_systems. Restart ES-DE to load them.';
  }

  @override
  String get installEsdeFailed => 'Unable to install ES-DE systems.';

  @override
  String get noMultiDiscGames => 'No multi-disc games found.';

  @override
  String get noZipCompatibleGames => 'No zip-compatible ROMs found.';

  @override
  String noCompatibleConverterFiles(Object tool) {
    return 'No compatible $tool files found.';
  }

  @override
  String get noVitaShortcutsQueued => 'No Vita shortcuts queued.';

  @override
  String get noScummVmGames => 'No ScummVM games detected.';

  @override
  String get noEsdeSystemsLoaded => 'No ES-DE systems loaded.';

  @override
  String get scanMultiDisc => 'Scanning for multi-disc games…';

  @override
  String get scanZipCompatible => 'Scanning for zip-compatible ROMs…';

  @override
  String scanConverter(Object tool) {
    return 'Scanning for $tool-compatible files…';
  }

  @override
  String get scanFiles => 'Scanning files…';

  @override
  String get scanScummVm => 'Detecting ScummVM games…';

  @override
  String filesChecked(int count) {
    return '$count files checked';
  }

  @override
  String organizingProgress(int completed, int total) {
    return 'Organizing ROMs: $completed of $total';
  }

  @override
  String zippingProgress(int completed, int total) {
    return 'Zipping ROMs: $completed of $total';
  }

  @override
  String convertingProgress(Object tool, int completed, int total) {
    return 'Converting with $tool: $completed of $total';
  }

  @override
  String creatingShortcutsProgress(int completed, int total) {
    return 'Creating shortcuts: $completed of $total';
  }

  @override
  String creatingScummVmProgress(int completed, int total) {
    return 'Creating ScummVM launchers: $completed of $total';
  }

  @override
  String updatingEsdeProgress(int completed, int total) {
    return 'Updating ES-DE systems: $completed of $total';
  }
}
