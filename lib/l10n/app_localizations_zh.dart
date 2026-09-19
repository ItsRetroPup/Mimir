// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Chinese (`zh`).
class AppLocalizationsZh extends AppLocalizations {
  AppLocalizationsZh([String locale = 'zh']) : super(locale);

  @override
  String get appName => 'Mimir';

  @override
  String get home => 'Home';

  @override
  String get goHome => 'Go home';

  @override
  String get toggleDarkMode => 'Toggle dark mode';

  @override
  String get status => 'Status';

  @override
  String get homeIntro => 'Welcome to Mimir!';

  @override
  String get homePrompt => 'Choose a tool from the list below to get started';

  @override
  String get homePrepareLibrary => 'Some handy tools';

  @override
  String get homeMakeRoom => 'Save space on your device';

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
  String get goalFixMultidisc => 'Fix multi-disc games';

  @override
  String get toolMultidisc => 'Multi-disc Organizer';

  @override
  String get toolMultidiscDescription =>
      'Create frontend-ready folders and playlists.';

  @override
  String get goalCreateVitaShortcuts => 'Create Vita shortcuts';

  @override
  String get toolVitaShortcuts => 'Vita Shortcuts';

  @override
  String get toolVitaShortcutsDescription =>
      'Make .psvita or .dpt shortcuts from the built-in database.';

  @override
  String get goalCreateScummVmLaunchers => 'Create ScummVM launchers';

  @override
  String get toolScummVmLaunchers => 'ScummVM Launchers';

  @override
  String get toolScummVmLaunchersDescription =>
      'Detect games and create .scummvm files beside them.';

  @override
  String get goalSetupEsde => 'Set up ES-DE';

  @override
  String get toolEsdeSystems => 'ES-DE Systems';

  @override
  String get toolEsdeSystemsDescription =>
      'Install custom systems and map their ROM folders.';

  @override
  String get goalConvertSwitchPackages => 'Convert Switch packages';

  @override
  String get toolNszToNsp => 'NSZ to NSP';

  @override
  String get toolNszToNspDescription =>
      'Decompress NSZ files into NSP packages.';

  @override
  String get goalCompressCartridgeRoms => 'Compress cartridge ROMs';

  @override
  String get toolRomzipper => 'RomZipper';

  @override
  String get toolRomzipperDescription =>
      'Archive supported cartridge ROMs as .zip files.';

  @override
  String get goalCompressDiscImages => 'Compress disc images';

  @override
  String get toolChdman => 'CHDMan';

  @override
  String get toolChdmanDescription =>
      'Convert PSX, PS2, PSP, Saturn, Dreamcast, and Sega CD images to CHD.';

  @override
  String get goalShrinkGamecubeWii => 'Shrink GameCube and Wii games';

  @override
  String get toolDolphinRvz => 'Dolphin RVZ';

  @override
  String get toolDolphinRvzDescription => 'Convert ISO images to RVZ.';

  @override
  String get goalShrink3ds => 'Shrink Nintendo 3DS games';

  @override
  String get toolAzaharZcci => 'Azahar ZCCI';

  @override
  String get toolAzaharZcciDescription =>
      'Compress decrypted 3DS and CCI images to ZCCI.';

  @override
  String toolCardDescription(Object tool, Object description) {
    return '$tool • $description';
  }

  @override
  String get noRomFolderSelected => 'No ROM folder selected';

  @override
  String get noVitaOutputSelected => 'No Vita output directory selected';

  @override
  String get noScummVmFolderSelected => 'No ScummVM Games folder selected';

  @override
  String get noEsdeFolderSelected => 'No ES-DE folder selected';

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

/// The translations for Chinese, as used in China (`zh_CN`).
class AppLocalizationsZhCn extends AppLocalizationsZh {
  AppLocalizationsZhCn() : super('zh_CN');

  @override
  String get appName => 'Mimir';

  @override
  String get home => '主页';

  @override
  String get goHome => '返回主页';

  @override
  String get toggleDarkMode => '切换深色模式';

  @override
  String get status => '状态';

  @override
  String get homeIntro => '欢迎使用 Mimir！';

  @override
  String get homePrompt => '从下方列表中选择一个工具开始使用';

  @override
  String get homePrepareLibrary => '实用工具';

  @override
  String get homeMakeRoom => '节省设备空间';

  @override
  String get goalFixMultidisc => '整理多光盘游戏';

  @override
  String get toolMultidisc => '多光盘整理工具';

  @override
  String get toolMultidiscDescription => '创建适用于前端的文件夹和播放列表。';

  @override
  String get goalCreateVitaShortcuts => '创建 Vita 快捷方式';

  @override
  String get toolVitaShortcuts => 'Vita 快捷方式';

  @override
  String get toolVitaShortcutsDescription => '从内置数据库创建 .psvita 或 .dpt 快捷方式。';

  @override
  String get goalSetupEsde => '设置 ES-DE';

  @override
  String get toolEsdeSystems => 'ES-DE 系统';

  @override
  String get toolEsdeSystemsDescription => '安装自定义系统并映射其 ROM 文件夹。';

  @override
  String get goalConvertSwitchPackages => '转换 Switch 软件包';

  @override
  String get toolNszToNsp => 'NSZ 转 NSP';

  @override
  String get toolNszToNspDescription => '将 NSZ 文件解压为 NSP 软件包。';

  @override
  String get goalCompressCartridgeRoms => '压缩卡带 ROM';

  @override
  String get toolRomzipper => 'RomZipper';

  @override
  String get toolRomzipperDescription => '将受支持的卡带 ROM 归档为 .zip 文件。';

  @override
  String get goalCompressDiscImages => '压缩光盘映像';

  @override
  String get toolChdman => 'CHDMan';

  @override
  String get toolChdmanDescription =>
      '将 PSX、PS2、PSP、Saturn、Dreamcast 和 Sega CD 映像转换为 CHD。';

  @override
  String get goalShrinkGamecubeWii => '压缩 GameCube 和 Wii 游戏';

  @override
  String get toolDolphinRvz => 'Dolphin RVZ';

  @override
  String get toolDolphinRvzDescription => '将 ISO 映像转换为 RVZ。';

  @override
  String get goalShrink3ds => '压缩 Nintendo 3DS 游戏';

  @override
  String get toolAzaharZcci => 'Azahar ZCCI';

  @override
  String get toolAzaharZcciDescription => '将已解密的 3DS 和 CCI 映像压缩为 ZCCI。';

  @override
  String get noRomFolderSelected => '未选择 ROM 文件夹';

  @override
  String get noVitaOutputSelected => '未选择 Vita 输出文件夹';

  @override
  String get noEsdeFolderSelected => '未选择 ES-DE 文件夹';
}

/// The translations for Chinese, as used in Taiwan (`zh_TW`).
class AppLocalizationsZhTw extends AppLocalizationsZh {
  AppLocalizationsZhTw() : super('zh_TW');

  @override
  String get appName => 'Mimir';

  @override
  String get home => '首頁';

  @override
  String get goHome => '返回首頁';

  @override
  String get toggleDarkMode => '切換深色模式';

  @override
  String get status => '狀態';

  @override
  String get homeIntro => '歡迎使用 Mimir！';

  @override
  String get homePrompt => '從下方清單中選擇工具開始使用';

  @override
  String get homePrepareLibrary => '實用工具';

  @override
  String get homeMakeRoom => '節省裝置空間';

  @override
  String get goalFixMultidisc => '整理多光碟遊戲';

  @override
  String get toolMultidisc => '多光碟整理工具';

  @override
  String get toolMultidiscDescription => '建立適用於前端的資料夾和播放清單。';

  @override
  String get goalCreateVitaShortcuts => '建立 Vita 捷徑';

  @override
  String get toolVitaShortcuts => 'Vita 捷徑';

  @override
  String get toolVitaShortcutsDescription => '從內建資料庫建立 .psvita 或 .dpt 捷徑。';

  @override
  String get goalSetupEsde => '設定 ES-DE';

  @override
  String get toolEsdeSystems => 'ES-DE 系統';

  @override
  String get toolEsdeSystemsDescription => '安裝自訂系統並對應其 ROM 資料夾。';

  @override
  String get goalConvertSwitchPackages => '轉換 Switch 套件';

  @override
  String get toolNszToNsp => 'NSZ 轉 NSP';

  @override
  String get toolNszToNspDescription => '將 NSZ 檔案解壓縮為 NSP 套件。';

  @override
  String get goalCompressCartridgeRoms => '壓縮卡帶 ROM';

  @override
  String get toolRomzipper => 'RomZipper';

  @override
  String get toolRomzipperDescription => '將支援的卡帶 ROM 封存為 .zip 檔案。';

  @override
  String get goalCompressDiscImages => '壓縮光碟映像';

  @override
  String get toolChdman => 'CHDMan';

  @override
  String get toolChdmanDescription =>
      '將 PSX、PS2、PSP、Saturn、Dreamcast 和 Sega CD 映像轉換為 CHD。';

  @override
  String get goalShrinkGamecubeWii => '壓縮 GameCube 和 Wii 遊戲';

  @override
  String get toolDolphinRvz => 'Dolphin RVZ';

  @override
  String get toolDolphinRvzDescription => '將 ISO 映像轉換為 RVZ。';

  @override
  String get goalShrink3ds => '壓縮 Nintendo 3DS 遊戲';

  @override
  String get toolAzaharZcci => 'Azahar ZCCI';

  @override
  String get toolAzaharZcciDescription => '將已解密的 3DS 和 CCI 映像壓縮為 ZCCI。';

  @override
  String get noRomFolderSelected => '未選擇 ROM 資料夾';

  @override
  String get noVitaOutputSelected => '未選擇 Vita 輸出資料夾';

  @override
  String get noEsdeFolderSelected => '未選擇 ES-DE 資料夾';
}
