import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_de.dart';
import 'app_localizations_en.dart';
import 'app_localizations_es.dart';
import 'app_localizations_fr.dart';
import 'app_localizations_it.dart';
import 'app_localizations_ja.dart';
import 'app_localizations_ko.dart';
import 'app_localizations_pl.dart';
import 'app_localizations_pt.dart';
import 'app_localizations_zh.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('de'),
    Locale('en'),
    Locale('es'),
    Locale('fr'),
    Locale('it'),
    Locale('ja'),
    Locale('ko'),
    Locale('pl'),
    Locale('pt'),
    Locale('pt', 'BR'),
    Locale('zh'),
    Locale('zh', 'CN'),
    Locale('zh', 'TW'),
  ];

  /// No description provided for @appName.
  ///
  /// In en, this message translates to:
  /// **'Mimir'**
  String get appName;

  /// No description provided for @home.
  ///
  /// In en, this message translates to:
  /// **'Home'**
  String get home;

  /// No description provided for @goHome.
  ///
  /// In en, this message translates to:
  /// **'Go home'**
  String get goHome;

  /// No description provided for @toggleDarkMode.
  ///
  /// In en, this message translates to:
  /// **'Toggle dark mode'**
  String get toggleDarkMode;

  /// No description provided for @status.
  ///
  /// In en, this message translates to:
  /// **'Status'**
  String get status;

  /// No description provided for @homeIntro.
  ///
  /// In en, this message translates to:
  /// **'Welcome to Mimir!'**
  String get homeIntro;

  /// No description provided for @homePrompt.
  ///
  /// In en, this message translates to:
  /// **'Choose a tool from the list below to get started'**
  String get homePrompt;

  /// No description provided for @homePrepareLibrary.
  ///
  /// In en, this message translates to:
  /// **'Some handy tools'**
  String get homePrepareLibrary;

  /// No description provided for @homeMakeRoom.
  ///
  /// In en, this message translates to:
  /// **'Save space on your device'**
  String get homeMakeRoom;

  /// No description provided for @supportMimir.
  ///
  /// In en, this message translates to:
  /// **'Support Mimir'**
  String get supportMimir;

  /// No description provided for @supportCopy.
  ///
  /// In en, this message translates to:
  /// **'Mimir is, and always will be, 100% free and without ads. If you would like to show your support, please consider checking out my YouTube channel or donating.'**
  String get supportCopy;

  /// No description provided for @youtube.
  ///
  /// In en, this message translates to:
  /// **'YouTube'**
  String get youtube;

  /// No description provided for @kofi.
  ///
  /// In en, this message translates to:
  /// **'Ko-fi'**
  String get kofi;

  /// No description provided for @buyMeACoffee.
  ///
  /// In en, this message translates to:
  /// **'Buy Me a Coffee'**
  String get buyMeACoffee;

  /// No description provided for @goalFixMultidisc.
  ///
  /// In en, this message translates to:
  /// **'Fix multi-disc games'**
  String get goalFixMultidisc;

  /// No description provided for @toolMultidisc.
  ///
  /// In en, this message translates to:
  /// **'Multi-disc Organizer'**
  String get toolMultidisc;

  /// No description provided for @toolMultidiscDescription.
  ///
  /// In en, this message translates to:
  /// **'Create frontend-ready folders and playlists.'**
  String get toolMultidiscDescription;

  /// No description provided for @goalCreateVitaShortcuts.
  ///
  /// In en, this message translates to:
  /// **'Create Vita shortcuts'**
  String get goalCreateVitaShortcuts;

  /// No description provided for @toolVitaShortcuts.
  ///
  /// In en, this message translates to:
  /// **'Vita Shortcuts'**
  String get toolVitaShortcuts;

  /// No description provided for @toolVitaShortcutsDescription.
  ///
  /// In en, this message translates to:
  /// **'Make .psvita or .dpt shortcuts from the built-in database.'**
  String get toolVitaShortcutsDescription;

  /// No description provided for @goalCreateScummVmLaunchers.
  ///
  /// In en, this message translates to:
  /// **'Create ScummVM launchers'**
  String get goalCreateScummVmLaunchers;

  /// No description provided for @toolScummVmLaunchers.
  ///
  /// In en, this message translates to:
  /// **'ScummVM Launchers'**
  String get toolScummVmLaunchers;

  /// No description provided for @toolScummVmLaunchersDescription.
  ///
  /// In en, this message translates to:
  /// **'Detect games and create .scummvm files beside them.'**
  String get toolScummVmLaunchersDescription;

  /// No description provided for @goalSetupEsde.
  ///
  /// In en, this message translates to:
  /// **'Set up ES-DE'**
  String get goalSetupEsde;

  /// No description provided for @toolEsdeSystems.
  ///
  /// In en, this message translates to:
  /// **'ES-DE Systems'**
  String get toolEsdeSystems;

  /// No description provided for @toolEsdeSystemsDescription.
  ///
  /// In en, this message translates to:
  /// **'Install custom systems and map their ROM folders.'**
  String get toolEsdeSystemsDescription;

  /// No description provided for @goalConvertSwitchPackages.
  ///
  /// In en, this message translates to:
  /// **'Convert Switch packages'**
  String get goalConvertSwitchPackages;

  /// No description provided for @toolNszToNsp.
  ///
  /// In en, this message translates to:
  /// **'NSZ to NSP'**
  String get toolNszToNsp;

  /// No description provided for @toolNszToNspDescription.
  ///
  /// In en, this message translates to:
  /// **'Decompress NSZ files into NSP packages.'**
  String get toolNszToNspDescription;

  /// No description provided for @goalCompressCartridgeRoms.
  ///
  /// In en, this message translates to:
  /// **'Compress cartridge ROMs'**
  String get goalCompressCartridgeRoms;

  /// No description provided for @toolRomzipper.
  ///
  /// In en, this message translates to:
  /// **'RomZipper'**
  String get toolRomzipper;

  /// No description provided for @toolRomzipperDescription.
  ///
  /// In en, this message translates to:
  /// **'Archive supported cartridge ROMs as .zip files.'**
  String get toolRomzipperDescription;

  /// No description provided for @goalCompressDiscImages.
  ///
  /// In en, this message translates to:
  /// **'Compress disc images'**
  String get goalCompressDiscImages;

  /// No description provided for @toolChdman.
  ///
  /// In en, this message translates to:
  /// **'CHDMan'**
  String get toolChdman;

  /// No description provided for @toolChdmanDescription.
  ///
  /// In en, this message translates to:
  /// **'Convert PSX, PS2, PSP, Saturn, Dreamcast, and Sega CD images to CHD.'**
  String get toolChdmanDescription;

  /// No description provided for @goalShrinkGamecubeWii.
  ///
  /// In en, this message translates to:
  /// **'Shrink GameCube and Wii games'**
  String get goalShrinkGamecubeWii;

  /// No description provided for @toolDolphinRvz.
  ///
  /// In en, this message translates to:
  /// **'Dolphin RVZ'**
  String get toolDolphinRvz;

  /// No description provided for @toolDolphinRvzDescription.
  ///
  /// In en, this message translates to:
  /// **'Convert ISO images to RVZ.'**
  String get toolDolphinRvzDescription;

  /// No description provided for @goalShrink3ds.
  ///
  /// In en, this message translates to:
  /// **'Shrink Nintendo 3DS games'**
  String get goalShrink3ds;

  /// No description provided for @toolAzaharZcci.
  ///
  /// In en, this message translates to:
  /// **'Azahar ZCCI'**
  String get toolAzaharZcci;

  /// No description provided for @toolAzaharZcciDescription.
  ///
  /// In en, this message translates to:
  /// **'Compress decrypted 3DS and CCI images to ZCCI.'**
  String get toolAzaharZcciDescription;

  /// No description provided for @toolCardDescription.
  ///
  /// In en, this message translates to:
  /// **'{tool} • {description}'**
  String toolCardDescription(Object tool, Object description);

  /// No description provided for @noRomFolderSelected.
  ///
  /// In en, this message translates to:
  /// **'No ROM folder selected'**
  String get noRomFolderSelected;

  /// No description provided for @noVitaOutputSelected.
  ///
  /// In en, this message translates to:
  /// **'No Vita output directory selected'**
  String get noVitaOutputSelected;

  /// No description provided for @noScummVmFolderSelected.
  ///
  /// In en, this message translates to:
  /// **'No ScummVM Games folder selected'**
  String get noScummVmFolderSelected;

  /// No description provided for @noEsdeFolderSelected.
  ///
  /// In en, this message translates to:
  /// **'No ES-DE folder selected'**
  String get noEsdeFolderSelected;

  /// No description provided for @selectedRomFolder.
  ///
  /// In en, this message translates to:
  /// **'Selected ROM folder'**
  String get selectedRomFolder;

  /// No description provided for @selectRomFolder.
  ///
  /// In en, this message translates to:
  /// **'Select ROM folder'**
  String get selectRomFolder;

  /// No description provided for @scummVmGamesFolder.
  ///
  /// In en, this message translates to:
  /// **'ScummVM Games folder'**
  String get scummVmGamesFolder;

  /// No description provided for @selectScummVmGamesFolder.
  ///
  /// In en, this message translates to:
  /// **'Select Games folder'**
  String get selectScummVmGamesFolder;

  /// No description provided for @scummVmExecutable.
  ///
  /// In en, this message translates to:
  /// **'ScummVM executable'**
  String get scummVmExecutable;

  /// No description provided for @scummVmExecutableAuto.
  ///
  /// In en, this message translates to:
  /// **'Auto-detected on scan; choose an app if needed.'**
  String get scummVmExecutableAuto;

  /// No description provided for @selectScummVmExecutable.
  ///
  /// In en, this message translates to:
  /// **'Choose the ScummVM app to run detection.'**
  String get selectScummVmExecutable;

  /// No description provided for @scummVmDescription.
  ///
  /// In en, this message translates to:
  /// **'Run --detect for each game folder and create a launcher file containing its game ID.'**
  String get scummVmDescription;

  /// No description provided for @shortcutOutputDirectory.
  ///
  /// In en, this message translates to:
  /// **'Shortcut output directory'**
  String get shortcutOutputDirectory;

  /// No description provided for @selectOutputFolder.
  ///
  /// In en, this message translates to:
  /// **'Select output folder'**
  String get selectOutputFolder;

  /// No description provided for @shortcutFileType.
  ///
  /// In en, this message translates to:
  /// **'Shortcut file type'**
  String get shortcutFileType;

  /// No description provided for @cocoonHint.
  ///
  /// In en, this message translates to:
  /// **'For Cocoon users, select .dpt'**
  String get cocoonHint;

  /// No description provided for @shortcutDatabase.
  ///
  /// In en, this message translates to:
  /// **'Shortcut database'**
  String get shortcutDatabase;

  /// No description provided for @titlesReady.
  ///
  /// In en, this message translates to:
  /// **'{count} titles ready for search'**
  String titlesReady(int count);

  /// No description provided for @searchTitleOrAppId.
  ///
  /// In en, this message translates to:
  /// **'Search title or app ID'**
  String get searchTitleOrAppId;

  /// No description provided for @searchResults.
  ///
  /// In en, this message translates to:
  /// **'Search results'**
  String get searchResults;

  /// No description provided for @quickAdd.
  ///
  /// In en, this message translates to:
  /// **'Quick add'**
  String get quickAdd;

  /// No description provided for @deleteShortcut.
  ///
  /// In en, this message translates to:
  /// **'Delete shortcut for {title}'**
  String deleteShortcut(Object title);

  /// No description provided for @selectedEsdeFolder.
  ///
  /// In en, this message translates to:
  /// **'Selected ES-DE folder'**
  String get selectedEsdeFolder;

  /// No description provided for @selectEsdeFolder.
  ///
  /// In en, this message translates to:
  /// **'Select ES-DE folder'**
  String get selectEsdeFolder;

  /// No description provided for @romRootFolder.
  ///
  /// In en, this message translates to:
  /// **'ROM root folder'**
  String get romRootFolder;

  /// No description provided for @selectRomRoot.
  ///
  /// In en, this message translates to:
  /// **'Select ROM root'**
  String get selectRomRoot;

  /// No description provided for @downloadLatestXmls.
  ///
  /// In en, this message translates to:
  /// **'Download latest XMLs'**
  String get downloadLatestXmls;

  /// No description provided for @installCustomSystems.
  ///
  /// In en, this message translates to:
  /// **'Install custom systems'**
  String get installCustomSystems;

  /// No description provided for @chooseFolder.
  ///
  /// In en, this message translates to:
  /// **'Choose folder'**
  String get chooseFolder;

  /// No description provided for @defaultLabel.
  ///
  /// In en, this message translates to:
  /// **'Default'**
  String get defaultLabel;

  /// No description provided for @esdeEmpty.
  ///
  /// In en, this message translates to:
  /// **'Download the latest XMLs to configure the custom Android systems. They will be installed under custom_systems.'**
  String get esdeEmpty;

  /// No description provided for @esdeFolderDescription.
  ///
  /// In en, this message translates to:
  /// **'Install the latest ES-DE Android custom systems into custom_systems.'**
  String get esdeFolderDescription;

  /// No description provided for @organizerDescription.
  ///
  /// In en, this message translates to:
  /// **'Organise multi-disc games into frontend-ready folders and playlists.'**
  String get organizerDescription;

  /// No description provided for @zipperDescription.
  ///
  /// In en, this message translates to:
  /// **'Compress supported ROM files into .zip archives to save space.'**
  String get zipperDescription;

  /// No description provided for @vitaDescription.
  ///
  /// In en, this message translates to:
  /// **'Create .psvita or .dpt shortcut files from the built-in Vita database.'**
  String get vitaDescription;

  /// No description provided for @frontEndTarget.
  ///
  /// In en, this message translates to:
  /// **'Frontend target'**
  String get frontEndTarget;

  /// No description provided for @otherFrontend.
  ///
  /// In en, this message translates to:
  /// **'Other'**
  String get otherFrontend;

  /// No description provided for @scanHiddenFolders.
  ///
  /// In en, this message translates to:
  /// **'Scan hidden folders'**
  String get scanHiddenFolders;

  /// No description provided for @scanHiddenDescription.
  ///
  /// In en, this message translates to:
  /// **'Extract ROMs from .-prefixed folders into the system folder before organizing.'**
  String get scanHiddenDescription;

  /// No description provided for @nszKeys.
  ///
  /// In en, this message translates to:
  /// **'NSZ keys'**
  String get nszKeys;

  /// No description provided for @nszKeysConfigured.
  ///
  /// In en, this message translates to:
  /// **'prod.keys is imported and will be kept in Mimir\'s private storage.'**
  String get nszKeysConfigured;

  /// No description provided for @nszKeysMissing.
  ///
  /// In en, this message translates to:
  /// **'Import your legally obtained prod.keys file before scanning or converting NSZ packages.'**
  String get nszKeysMissing;

  /// No description provided for @importProdKeys.
  ///
  /// In en, this message translates to:
  /// **'Import prod.keys'**
  String get importProdKeys;

  /// No description provided for @replaceProdKeys.
  ///
  /// In en, this message translates to:
  /// **'Replace prod.keys'**
  String get replaceProdKeys;

  /// No description provided for @system.
  ///
  /// In en, this message translates to:
  /// **'System'**
  String get system;

  /// No description provided for @conversionType.
  ///
  /// In en, this message translates to:
  /// **'Conversion type'**
  String get conversionType;

  /// No description provided for @compatibilityGuidance.
  ///
  /// In en, this message translates to:
  /// **'Compatibility guidance'**
  String get compatibilityGuidance;

  /// No description provided for @ps2Guidance.
  ///
  /// In en, this message translates to:
  /// **'Use CD when using NetherSX2; use DVD when using ARMSX2.'**
  String get ps2Guidance;

  /// No description provided for @preview.
  ///
  /// In en, this message translates to:
  /// **'Preview'**
  String get preview;

  /// No description provided for @previewSummary.
  ///
  /// In en, this message translates to:
  /// **'{count} multi-disc sets detected; {selected} selected.'**
  String previewSummary(int count, int selected);

  /// No description provided for @zipPreviewSummary.
  ///
  /// In en, this message translates to:
  /// **'{count} ROMs matched the zip whitelist; {selected} selected.'**
  String zipPreviewSummary(int count, int selected);

  /// No description provided for @jobsSelected.
  ///
  /// In en, this message translates to:
  /// **'{count} jobs selected • {size}'**
  String jobsSelected(int count, Object size);

  /// No description provided for @scanSelectedFolder.
  ///
  /// In en, this message translates to:
  /// **'Scan selected folder'**
  String get scanSelectedFolder;

  /// No description provided for @applySelected.
  ///
  /// In en, this message translates to:
  /// **'Apply {count} selected'**
  String applySelected(int count);

  /// No description provided for @convertSelected.
  ///
  /// In en, this message translates to:
  /// **'Convert {count} selected'**
  String convertSelected(int count);

  /// No description provided for @applyChanges.
  ///
  /// In en, this message translates to:
  /// **'Apply changes'**
  String get applyChanges;

  /// No description provided for @cancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get cancel;

  /// No description provided for @selectAll.
  ///
  /// In en, this message translates to:
  /// **'Select all'**
  String get selectAll;

  /// No description provided for @deselectAll.
  ///
  /// In en, this message translates to:
  /// **'Deselect all'**
  String get deselectAll;

  /// No description provided for @conversionQueue.
  ///
  /// In en, this message translates to:
  /// **'Conversion queue'**
  String get conversionQueue;

  /// No description provided for @sortBy.
  ///
  /// In en, this message translates to:
  /// **'Sort by'**
  String get sortBy;

  /// No description provided for @storageOnDevice.
  ///
  /// In en, this message translates to:
  /// **'Storage on scanned device'**
  String get storageOnDevice;

  /// No description provided for @romHeader.
  ///
  /// In en, this message translates to:
  /// **'ROM'**
  String get romHeader;

  /// No description provided for @sizeHeader.
  ///
  /// In en, this message translates to:
  /// **'SIZE'**
  String get sizeHeader;

  /// No description provided for @outputHeader.
  ///
  /// In en, this message translates to:
  /// **'OUTPUT'**
  String get outputHeader;

  /// No description provided for @existingOutput.
  ///
  /// In en, this message translates to:
  /// **'Existing output — select individually to replace'**
  String get existingOutput;

  /// No description provided for @replaceExistingOutput.
  ///
  /// In en, this message translates to:
  /// **'Replace existing output?'**
  String get replaceExistingOutput;

  /// No description provided for @replaceOutputBody.
  ///
  /// In en, this message translates to:
  /// **'{path} already exists and will be overwritten if selected.'**
  String replaceOutputBody(Object path);

  /// No description provided for @replaceOutput.
  ///
  /// In en, this message translates to:
  /// **'Replace output'**
  String get replaceOutput;

  /// No description provided for @sourceLabel.
  ///
  /// In en, this message translates to:
  /// **'Sources'**
  String get sourceLabel;

  /// No description provided for @targetLabel.
  ///
  /// In en, this message translates to:
  /// **'Targets'**
  String get targetLabel;

  /// No description provided for @noChanges.
  ///
  /// In en, this message translates to:
  /// **'No changes found.'**
  String get noChanges;

  /// No description provided for @selectFolderFirst.
  ///
  /// In en, this message translates to:
  /// **'Select a ROM folder first.'**
  String get selectFolderFirst;

  /// No description provided for @importKeysFirst.
  ///
  /// In en, this message translates to:
  /// **'Import a prod.keys file before scanning NSZ packages.'**
  String get importKeysFirst;

  /// No description provided for @changesApplied.
  ///
  /// In en, this message translates to:
  /// **'Changes applied.'**
  String get changesApplied;

  /// No description provided for @operationFailed.
  ///
  /// In en, this message translates to:
  /// **'Operation failed.'**
  String get operationFailed;

  /// No description provided for @conversionReport.
  ///
  /// In en, this message translates to:
  /// **'Conversion report'**
  String get conversionReport;

  /// No description provided for @supportDescription.
  ///
  /// In en, this message translates to:
  /// **'Mimir is, and always will be, 100% free and without ads. If you would like to show your support, please consider checking out my YouTube channel or donating.'**
  String get supportDescription;

  /// No description provided for @zipWhitelistInfo.
  ///
  /// In en, this message translates to:
  /// **'Supported extensions are selected by the current whitelist. Existing .zip outputs are skipped safely.'**
  String get zipWhitelistInfo;

  /// No description provided for @discType.
  ///
  /// In en, this message translates to:
  /// **'Disc type'**
  String get discType;

  /// No description provided for @deleteOriginalFiles.
  ///
  /// In en, this message translates to:
  /// **'Delete original files after a successful conversion'**
  String get deleteOriginalFiles;

  /// No description provided for @conflictsSkipped.
  ///
  /// In en, this message translates to:
  /// **'{count} conflicting outputs were skipped.'**
  String conflictsSkipped(int count);

  /// No description provided for @applyConfirmationBody.
  ///
  /// In en, this message translates to:
  /// **'Mimir will execute the selected changes sequentially. Partial completion will be reported if you stop or an operation fails.'**
  String get applyConfirmationBody;

  /// No description provided for @working.
  ///
  /// In en, this message translates to:
  /// **'Working…'**
  String get working;

  /// No description provided for @stopAfterCurrent.
  ///
  /// In en, this message translates to:
  /// **'Stop after current'**
  String get stopAfterCurrent;

  /// No description provided for @stopNow.
  ///
  /// In en, this message translates to:
  /// **'Stop now'**
  String get stopNow;

  /// No description provided for @noMatchingTitles.
  ///
  /// In en, this message translates to:
  /// **'No matching titles.'**
  String get noMatchingTitles;

  /// No description provided for @systemsCount.
  ///
  /// In en, this message translates to:
  /// **'{count} systems'**
  String systemsCount(int count);

  /// No description provided for @prodKeysImported.
  ///
  /// In en, this message translates to:
  /// **'prod.keys imported and saved on this device.'**
  String get prodKeysImported;

  /// No description provided for @importKeysFailed.
  ///
  /// In en, this message translates to:
  /// **'Unable to import prod.keys.'**
  String get importKeysFailed;

  /// No description provided for @selectRomFolderFirst.
  ///
  /// In en, this message translates to:
  /// **'Select a ROM folder first.'**
  String get selectRomFolderFirst;

  /// No description provided for @selectScummVmFolderFirst.
  ///
  /// In en, this message translates to:
  /// **'Select a ScummVM Games folder first.'**
  String get selectScummVmFolderFirst;

  /// No description provided for @scummVmExecutableNotFound.
  ///
  /// In en, this message translates to:
  /// **'ScummVM was not found. Choose the app file to continue.'**
  String get scummVmExecutableNotFound;

  /// No description provided for @scummVmDesktopOnly.
  ///
  /// In en, this message translates to:
  /// **'ScummVM executable detection is currently available on desktop platforms only.'**
  String get scummVmDesktopOnly;

  /// No description provided for @toolDoesNotUseRomScanning.
  ///
  /// In en, this message translates to:
  /// **'This tool does not use ROM scanning.'**
  String get toolDoesNotUseRomScanning;

  /// No description provided for @scanStopped.
  ///
  /// In en, this message translates to:
  /// **'Scan stopped.'**
  String get scanStopped;

  /// No description provided for @scanFailed.
  ///
  /// In en, this message translates to:
  /// **'Scan failed.'**
  String get scanFailed;

  /// No description provided for @operationStopped.
  ///
  /// In en, this message translates to:
  /// **'Operation stopped.'**
  String get operationStopped;

  /// No description provided for @stoppedAfterOperations.
  ///
  /// In en, this message translates to:
  /// **'Stopped after {completed} of {total} operations.'**
  String stoppedAfterOperations(int completed, int total);

  /// No description provided for @applyFailed.
  ///
  /// In en, this message translates to:
  /// **'Apply failed.'**
  String get applyFailed;

  /// No description provided for @stopping.
  ///
  /// In en, this message translates to:
  /// **'Stopping…'**
  String get stopping;

  /// No description provided for @stopAfterCurrentMessage.
  ///
  /// In en, this message translates to:
  /// **'Will stop after the current conversion.'**
  String get stopAfterCurrentMessage;

  /// No description provided for @selectVitaOutputFirst.
  ///
  /// In en, this message translates to:
  /// **'Select a Vita shortcut output folder first.'**
  String get selectVitaOutputFirst;

  /// No description provided for @unableToAddShortcut.
  ///
  /// In en, this message translates to:
  /// **'Unable to add shortcut for {title}.'**
  String unableToAddShortcut(Object title);

  /// No description provided for @addedShortcut.
  ///
  /// In en, this message translates to:
  /// **'Added shortcut: {title}'**
  String addedShortcut(Object title);

  /// No description provided for @addShortcutFailed.
  ///
  /// In en, this message translates to:
  /// **'Add shortcut failed.'**
  String get addShortcutFailed;

  /// No description provided for @removedShortcut.
  ///
  /// In en, this message translates to:
  /// **'Removed shortcut: {title}'**
  String removedShortcut(Object title);

  /// No description provided for @removeShortcutFailed.
  ///
  /// In en, this message translates to:
  /// **'Remove shortcut failed.'**
  String get removeShortcutFailed;

  /// No description provided for @selectEsdeFolderFirst.
  ///
  /// In en, this message translates to:
  /// **'Select the ES-DE folder first.'**
  String get selectEsdeFolderFirst;

  /// No description provided for @selectRomRootForEsde.
  ///
  /// In en, this message translates to:
  /// **'Select the ROM root folder first so Mimir can find its systems.'**
  String get selectRomRootForEsde;

  /// No description provided for @downloadingEsde.
  ///
  /// In en, this message translates to:
  /// **'Downloading the latest ES-DE custom systems…'**
  String get downloadingEsde;

  /// No description provided for @downloadedEsde.
  ///
  /// In en, this message translates to:
  /// **'Downloaded {count} ES-DE systems. Review the folders, then apply them.'**
  String downloadedEsde(int count);

  /// No description provided for @downloadEsdeFailed.
  ///
  /// In en, this message translates to:
  /// **'Unable to download ES-DE systems.'**
  String get downloadEsdeFailed;

  /// No description provided for @installingEsde.
  ///
  /// In en, this message translates to:
  /// **'Installing ES-DE custom systems…'**
  String get installingEsde;

  /// No description provided for @installedEsde.
  ///
  /// In en, this message translates to:
  /// **'Installed {count} systems in custom_systems. Restart ES-DE to load them.'**
  String installedEsde(int count);

  /// No description provided for @installEsdeFailed.
  ///
  /// In en, this message translates to:
  /// **'Unable to install ES-DE systems.'**
  String get installEsdeFailed;

  /// No description provided for @noMultiDiscGames.
  ///
  /// In en, this message translates to:
  /// **'No multi-disc games found.'**
  String get noMultiDiscGames;

  /// No description provided for @noZipCompatibleGames.
  ///
  /// In en, this message translates to:
  /// **'No zip-compatible ROMs found.'**
  String get noZipCompatibleGames;

  /// No description provided for @noCompatibleConverterFiles.
  ///
  /// In en, this message translates to:
  /// **'No compatible {tool} files found.'**
  String noCompatibleConverterFiles(Object tool);

  /// No description provided for @noVitaShortcutsQueued.
  ///
  /// In en, this message translates to:
  /// **'No Vita shortcuts queued.'**
  String get noVitaShortcutsQueued;

  /// No description provided for @noScummVmGames.
  ///
  /// In en, this message translates to:
  /// **'No ScummVM games detected.'**
  String get noScummVmGames;

  /// No description provided for @noEsdeSystemsLoaded.
  ///
  /// In en, this message translates to:
  /// **'No ES-DE systems loaded.'**
  String get noEsdeSystemsLoaded;

  /// No description provided for @scanMultiDisc.
  ///
  /// In en, this message translates to:
  /// **'Scanning for multi-disc games…'**
  String get scanMultiDisc;

  /// No description provided for @scanZipCompatible.
  ///
  /// In en, this message translates to:
  /// **'Scanning for zip-compatible ROMs…'**
  String get scanZipCompatible;

  /// No description provided for @scanConverter.
  ///
  /// In en, this message translates to:
  /// **'Scanning for {tool}-compatible files…'**
  String scanConverter(Object tool);

  /// No description provided for @scanFiles.
  ///
  /// In en, this message translates to:
  /// **'Scanning files…'**
  String get scanFiles;

  /// No description provided for @scanScummVm.
  ///
  /// In en, this message translates to:
  /// **'Detecting ScummVM games…'**
  String get scanScummVm;

  /// No description provided for @filesChecked.
  ///
  /// In en, this message translates to:
  /// **'{count} files checked'**
  String filesChecked(int count);

  /// No description provided for @organizingProgress.
  ///
  /// In en, this message translates to:
  /// **'Organizing ROMs: {completed} of {total}'**
  String organizingProgress(int completed, int total);

  /// No description provided for @zippingProgress.
  ///
  /// In en, this message translates to:
  /// **'Zipping ROMs: {completed} of {total}'**
  String zippingProgress(int completed, int total);

  /// No description provided for @convertingProgress.
  ///
  /// In en, this message translates to:
  /// **'Converting with {tool}: {completed} of {total}'**
  String convertingProgress(Object tool, int completed, int total);

  /// No description provided for @creatingShortcutsProgress.
  ///
  /// In en, this message translates to:
  /// **'Creating shortcuts: {completed} of {total}'**
  String creatingShortcutsProgress(int completed, int total);

  /// No description provided for @creatingScummVmProgress.
  ///
  /// In en, this message translates to:
  /// **'Creating ScummVM launchers: {completed} of {total}'**
  String creatingScummVmProgress(int completed, int total);

  /// No description provided for @updatingEsdeProgress.
  ///
  /// In en, this message translates to:
  /// **'Updating ES-DE systems: {completed} of {total}'**
  String updatingEsdeProgress(int completed, int total);
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) => <String>[
    'de',
    'en',
    'es',
    'fr',
    'it',
    'ja',
    'ko',
    'pl',
    'pt',
    'zh',
  ].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when language+country codes are specified.
  switch (locale.languageCode) {
    case 'pt':
      {
        switch (locale.countryCode) {
          case 'BR':
            return AppLocalizationsPtBr();
        }
        break;
      }
    case 'zh':
      {
        switch (locale.countryCode) {
          case 'CN':
            return AppLocalizationsZhCn();
          case 'TW':
            return AppLocalizationsZhTw();
        }
        break;
      }
  }

  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'de':
      return AppLocalizationsDe();
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
    case 'fr':
      return AppLocalizationsFr();
    case 'it':
      return AppLocalizationsIt();
    case 'ja':
      return AppLocalizationsJa();
    case 'ko':
      return AppLocalizationsKo();
    case 'pl':
      return AppLocalizationsPl();
    case 'pt':
      return AppLocalizationsPt();
    case 'zh':
      return AppLocalizationsZh();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
