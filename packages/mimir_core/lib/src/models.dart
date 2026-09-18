import 'package:meta/meta.dart';

enum FrontendLayout {
  folderAsFile,
  playlistAtRootWithoutMoves,
  playlistAtRootWithDiscFolder,
}

enum FrontendPreset { esDe, other }

extension FrontendPresetInfo on FrontendPreset {
  String get displayName => switch (this) {
    FrontendPreset.esDe => 'ES-DE',
    FrontendPreset.other => 'Other',
  };

  FrontendLayout get layout => switch (this) {
    FrontendPreset.esDe => FrontendLayout.folderAsFile,
    FrontendPreset.other => FrontendLayout.playlistAtRootWithoutMoves,
  };
}

enum VitaShortcutFormat { psvita, dpt }

extension VitaShortcutFormatInfo on VitaShortcutFormat {
  String get extension => switch (this) {
    VitaShortcutFormat.psvita => 'psvita',
    VitaShortcutFormat.dpt => 'dpt',
  };

  String get displayName => switch (this) {
    VitaShortcutFormat.psvita => '.psvita',
    VitaShortcutFormat.dpt => '.dpt',
  };
}

enum ToolMode {
  multiDiscOrganizer,
  romZipper,
  chdConverter,
  vitaAppIds,
  esDeSystems,
}

extension ToolModeInfo on ToolMode {
  String get displayName => switch (this) {
    ToolMode.multiDiscOrganizer => 'Multi-disc Organizer',
    ToolMode.romZipper => 'RomZipper',
    ToolMode.chdConverter => 'Converter Tools',
    ToolMode.vitaAppIds => 'Vita App IDs',
    ToolMode.esDeSystems => 'ES-DE Systems',
  };
}

enum ConverterTool { chd, dolphinRvz, azaharZcci, nszNsp }

extension ConverterToolInfo on ConverterTool {
  String get displayName => switch (this) {
    ConverterTool.chd => 'CHDMan',
    ConverterTool.dolphinRvz => 'Dolphin RVZ',
    ConverterTool.azaharZcci => 'Azahar ZCCI',
    ConverterTool.nszNsp => 'NSZ to NSP',
  };

  String get description => switch (this) {
    ConverterTool.chd => 'Disc images to space-saving CHD files',
    ConverterTool.dolphinRvz => 'GameCube and Wii ISO images to RVZ',
    ConverterTool.azaharZcci => 'Decrypted 3DS/CCI images to compressed ZCCI',
    ConverterTool.nszNsp => 'Decompress Nintendo Switch NSZ packages to NSP',
  };

  Set<String> get sourceExtensions => switch (this) {
    ConverterTool.chd => const <String>{},
    ConverterTool.dolphinRvz => const {'iso'},
    ConverterTool.azaharZcci => const {'3ds', 'cci'},
    ConverterTool.nszNsp => const {'nsz'},
  };

  String get outputExtension => switch (this) {
    ConverterTool.chd => 'chd',
    ConverterTool.dolphinRvz => 'rvz',
    ConverterTool.azaharZcci => 'zcci',
    ConverterTool.nszNsp => 'nsp',
  };

  Set<String> get folderAliases => switch (this) {
    ConverterTool.chd => const {},
    ConverterTool.dolphinRvz => const {
      'gamecube',
      'gc',
      'nintendo gamecube',
      'wii',
      'nintendo wii',
    },
    ConverterTool.azaharZcci => const {'3ds', 'nintendo 3ds'},
    ConverterTool.nszNsp => const {'switch', 'nintendo switch'},
  };

  String get executableName => switch (this) {
    ConverterTool.chd => 'libchdman.so',
    ConverterTool.dolphinRvz => 'libdolphintool.so',
    ConverterTool.azaharZcci => 'libazahar.so',
    ConverterTool.nszNsp => '',
  };
}

enum ChdDiscType { cd, dvd }

extension ChdDiscTypeInfo on ChdDiscType {
  String get displayName => switch (this) {
    ChdDiscType.cd => 'CD',
    ChdDiscType.dvd => 'DVD',
  };

  String get commandName => switch (this) {
    ChdDiscType.cd => 'createcd',
    ChdDiscType.dvd => 'createdvd',
  };
}

enum ChdSystem {
  dreamcast,
  playStation1,
  playStation2,
  segaCd,
  segaSaturn,
  playStationPortable,
}

extension ChdSystemInfo on ChdSystem {
  String get displayName => switch (this) {
    ChdSystem.dreamcast => 'Dreamcast',
    ChdSystem.playStation1 => 'PS1',
    ChdSystem.playStation2 => 'PS2',
    ChdSystem.segaCd => 'Sega CD',
    ChdSystem.segaSaturn => 'Sega Saturn',
    ChdSystem.playStationPortable => 'PSP',
  };

  Set<String> get supportedExtensions => switch (this) {
    ChdSystem.dreamcast => const {'gdi', 'cue', 'iso'},
    ChdSystem.playStation1 => const {'cue', 'iso'},
    ChdSystem.playStation2 => const {'iso'},
    ChdSystem.segaCd => const {'cue', 'iso'},
    ChdSystem.segaSaturn => const {'cue', 'iso'},
    ChdSystem.playStationPortable => const {'iso'},
  };

  Set<String> get folderAliases => switch (this) {
    ChdSystem.dreamcast => const {'dreamcast', 'dc'},
    ChdSystem.playStation1 => const {'playstation 1', 'ps1', 'psx'},
    ChdSystem.playStation2 => const {'playstation 2', 'ps2'},
    ChdSystem.segaCd => const {'sega cd', 'segacd', 'mega cd', 'megacd'},
    ChdSystem.segaSaturn => const {'sega saturn', 'saturn'},
    ChdSystem.playStationPortable => const {'playstation portable', 'psp'},
  };
}

@immutable
class RomEntry {
  const RomEntry({
    required this.relativePath,
    required this.fileName,
    this.sourcePath,
    this.sizeBytes = 0,
  });

  final String relativePath;
  final String fileName;
  final String? sourcePath;
  final int sizeBytes;

  String get resolvedSourcePath => sourcePath ?? relativePath;
}

@immutable
class VitaApp {
  const VitaApp({
    required this.titleId,
    required this.title,
    required this.sourcePath,
  });

  final String titleId;
  final String title;
  final String sourcePath;
}

@immutable
class DiscMatch {
  const DiscMatch({required this.title, required this.discNumber});

  final String title;
  final int discNumber;
}

@immutable
class DiscGameSet {
  const DiscGameSet({
    required this.title,
    required this.parentPath,
    required this.entries,
  });

  final String title;
  final String parentPath;
  final List<RomEntry> entries;
}

@immutable
class ScanResult {
  const ScanResult({
    required this.totalFiles,
    required this.allEntries,
    required this.discSets,
  });

  final int totalFiles;
  final List<RomEntry> allEntries;
  final List<DiscGameSet> discSets;
}

sealed class FileOperation {
  const FileOperation();
}

final class CreateDirectory extends FileOperation {
  const CreateDirectory(this.relativePath);

  final String relativePath;
}

final class MoveFile extends FileOperation {
  const MoveFile({required this.sourcePath, required this.targetPath});

  final String sourcePath;
  final String targetPath;
}

final class WriteTextFile extends FileOperation {
  const WriteTextFile({required this.relativePath, required this.contents});

  final String relativePath;
  final String contents;
}

final class ZipFile extends FileOperation {
  const ZipFile({
    required this.sourcePath,
    required this.targetPath,
    required this.archiveEntryName,
  });

  final String sourcePath;
  final String targetPath;
  final String archiveEntryName;
}

final class ConvertToChd extends FileOperation {
  const ConvertToChd({
    required this.sourcePath,
    required this.targetPath,
    required this.system,
    required this.discType,
    required this.deleteOriginalFiles,
  });

  final String sourcePath;
  final String targetPath;
  final ChdSystem system;
  final ChdDiscType discType;
  final bool deleteOriginalFiles;
}

final class ConvertWithTool extends FileOperation {
  const ConvertWithTool({
    required this.sourcePath,
    required this.targetPath,
    required this.tool,
  });

  final String sourcePath;
  final String targetPath;
  final ConverterTool tool;
}

@immutable
class PlannedChange {
  const PlannedChange({
    required this.title,
    required this.sourceFiles,
    required this.targetFiles,
    required this.detailLabel,
    required this.detailPath,
    required this.operations,
    this.sourceSizeBytes = 0,
    this.targetAlreadyExists = false,
  });

  final String title;
  final List<String> sourceFiles;
  final List<String> targetFiles;
  final String detailLabel;
  final String detailPath;
  final List<FileOperation> operations;
  final int sourceSizeBytes;
  final bool targetAlreadyExists;
}

@immutable
class OperationPlan {
  const OperationPlan({
    required this.mode,
    required this.changes,
    required this.operations,
    required this.conflicts,
    this.preset,
  });

  final ToolMode mode;
  final FrontendPreset? preset;
  final List<PlannedChange> changes;
  final List<FileOperation> operations;
  final List<String> conflicts;

  OperationPlan forSelectedChanges(Set<String> selectedDetailPaths) {
    final selectedChanges = changes
        .where((change) => selectedDetailPaths.contains(change.detailPath))
        .toList();
    return OperationPlan(
      mode: mode,
      preset: preset,
      changes: selectedChanges,
      operations: selectedChanges
          .expand((change) => change.operations)
          .toList(),
      conflicts: conflicts,
    );
  }
}

@immutable
class OperationEventData {
  const OperationEventData({
    required this.completed,
    required this.total,
    this.current,
    this.currentProgress,
    this.spaceSavedBytes = 0,
    this.stopped = false,
    this.finished = false,
    this.error,
  });

  final int completed;
  final int total;
  final String? current;
  final double? currentProgress;
  final int spaceSavedBytes;
  final bool stopped;
  final bool finished;
  final String? error;
}
