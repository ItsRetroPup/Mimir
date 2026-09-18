import 'models.dart';
import 'relative_paths.dart';

class ChangePlanner {
  ChangePlanner._();

  static OperationPlan buildPlan({
    required ScanResult scanResult,
    required FrontendPreset preset,
  }) {
    final operations = <FileOperation>[];
    final changes = <PlannedChange>[];
    final conflicts = <String>[];
    final existingPaths = scanResult.allEntries
        .map((entry) => entry.relativePath)
        .toSet();
    final reservedPaths = <String>{};

    for (final discSet in scanResult.discSets) {
      final plan =
          discSet.parentPath
              .split('/')
              .any((part) => part.toLowerCase() == 'ps2')
          ? _buildPs2Plan(discSet)
          : switch (preset.layout) {
              FrontendLayout.folderAsFile => _buildFolderAsFilePlan(discSet),
              FrontendLayout.playlistAtRootWithoutMoves =>
                _buildRootPlaylistWithoutMovesPlan(discSet),
              FrontendLayout.playlistAtRootWithDiscFolder =>
                _buildRootPlaylistPlan(discSet),
            };

      final discSetConflicts = <String>[];
      final allTargets = [...plan.createdDirectories, ...plan.createdFiles];
      for (final path in allTargets) {
        if (existingPaths.contains(path) || !reservedPaths.add(path)) {
          discSetConflicts.add(
            'Skipped ${discSet.title}: target already exists: $path',
          );
        }
      }
      if (discSetConflicts.isNotEmpty) {
        conflicts.addAll(discSetConflicts);
        continue;
      }

      final changeOperations = <FileOperation>[
        ...plan.createdDirectories.map(CreateDirectory.new),
        ...plan.moves.map(
          (move) => MoveFile(sourcePath: move.$1, targetPath: move.$2),
        ),
        if (plan.playlistPath != null)
          WriteTextFile(
            relativePath: plan.playlistPath!,
            contents: plan.playlistContents ?? '',
          ),
      ];
      operations.addAll(changeOperations);
      changes.add(
        PlannedChange(
          title: discSet.title,
          sourceFiles: discSet.entries
              .map((entry) => entry.relativePath)
              .toList(),
          targetFiles: plan.targetFiles,
          detailLabel: plan.playlistPath == null ? 'Disc folder' : 'Playlist',
          detailPath: plan.playlistPath ?? plan.createdDirectories.single,
          operations: changeOperations,
        ),
      );
    }
    return OperationPlan(
      mode: ToolMode.multiDiscOrganizer,
      preset: preset,
      changes: changes,
      operations: operations,
      conflicts: conflicts.toSet().toList(),
    );
  }

  static _InternalPlan _buildFolderAsFilePlan(DiscGameSet discSet) {
    final folderName = '${discSet.title}.m3u';
    final containerPath = RelativePaths.join(discSet.parentPath, folderName);
    final playlistPath = RelativePaths.join(
      containerPath,
      '${discSet.title}.m3u',
    );
    final moves = discSet.entries.map((entry) {
      return (
        entry.resolvedSourcePath,
        RelativePaths.join(containerPath, entry.fileName),
      );
    }).toList();
    final playlistContents = moves
        .where((move) => !_hasExtension(RelativePaths.nameOf(move.$2), 'bin'))
        .map((move) => RelativePaths.nameOf(move.$2))
        .join('\n');
    return _InternalPlan(
      createdDirectories: [containerPath],
      moves: moves,
      targetFiles: moves.map((move) => move.$2).toList(),
      createdFiles: [...moves.map((move) => move.$2), playlistPath],
      playlistPath: playlistPath,
      playlistContents: playlistContents,
    );
  }

  static _InternalPlan _buildRootPlaylistPlan(DiscGameSet discSet) {
    final discFolder = RelativePaths.join(discSet.parentPath, discSet.title);
    final playlistPath = RelativePaths.join(
      discSet.parentPath,
      '${discSet.title}.m3u',
    );
    final moves = discSet.entries.map((entry) {
      return (
        entry.resolvedSourcePath,
        RelativePaths.join(discFolder, entry.fileName),
      );
    }).toList();
    final folderName = RelativePaths.nameOf(discFolder);
    final playlistContents = moves
        .where((move) => !_hasExtension(RelativePaths.nameOf(move.$2), 'bin'))
        .map((move) => '$folderName/${RelativePaths.nameOf(move.$2)}')
        .join('\n');
    return _InternalPlan(
      createdDirectories: [discFolder],
      moves: moves,
      targetFiles: moves.map((move) => move.$2).toList(),
      createdFiles: [...moves.map((move) => move.$2), playlistPath],
      playlistPath: playlistPath,
      playlistContents: playlistContents,
    );
  }

  static _InternalPlan _buildRootPlaylistWithoutMovesPlan(DiscGameSet discSet) {
    final playlistPath = RelativePaths.join(
      discSet.parentPath,
      '${discSet.title}.m3u',
    );
    final playlistContents = discSet.entries
        .where((entry) => !_hasExtension(entry.fileName, 'bin'))
        .map((entry) => entry.fileName)
        .join('\n');
    return _InternalPlan(
      createdDirectories: const [],
      moves: const [],
      targetFiles: discSet.entries.map((entry) => entry.relativePath).toList(),
      createdFiles: [playlistPath],
      playlistPath: playlistPath,
      playlistContents: playlistContents,
    );
  }

  static _InternalPlan _buildPs2Plan(DiscGameSet discSet) {
    final discFolder = RelativePaths.join(discSet.parentPath, discSet.title);
    final moves = discSet.entries.map((entry) {
      return (
        entry.resolvedSourcePath,
        RelativePaths.join(discFolder, entry.fileName),
      );
    }).toList();
    return _InternalPlan(
      createdDirectories: [discFolder],
      moves: moves,
      targetFiles: moves.map((move) => move.$2).toList(),
      createdFiles: moves.map((move) => move.$2).toList(),
      playlistPath: null,
      playlistContents: null,
    );
  }

  static bool _hasExtension(String value, String extension) {
    final dot = value.lastIndexOf('.');
    return dot >= 0 &&
        value.substring(dot + 1).toLowerCase() == extension.toLowerCase();
  }
}

class _InternalPlan {
  const _InternalPlan({
    required this.createdDirectories,
    required this.moves,
    required this.targetFiles,
    required this.createdFiles,
    required this.playlistPath,
    required this.playlistContents,
  });

  final List<String> createdDirectories;
  final List<(String, String)> moves;
  final List<String> targetFiles;
  final List<String> createdFiles;
  final String? playlistPath;
  final String? playlistContents;
}
