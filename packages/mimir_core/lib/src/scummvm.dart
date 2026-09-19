import 'models.dart';
import 'relative_paths.dart';

/// A game folder and the id reported by ScummVM's `--detect` command.
class ScummVmGame {
  const ScummVmGame({
    required this.folderPath,
    required this.gameName,
    required this.gameId,
    this.targetAlreadyExists = false,
  });

  /// Path relative to the selected ScummVM Games folder.
  final String folderPath;
  final String gameName;
  final String gameId;
  final bool targetAlreadyExists;

  String get launcherPath =>
      RelativePaths.join(folderPath, '$gameName.scummvm');
}

/// Parses the human-readable output produced by `scummvm --detect`.
class ScummVmDetector {
  ScummVmDetector._();

  static final _qualifiedId = RegExp(
    r'(?<![A-Za-z0-9_-])([A-Za-z0-9_-]+:[A-Za-z0-9_-]+)(?![A-Za-z0-9_-])',
  );
  static final _labelledId = RegExp(
    r'\b(?:detected\s+game|game\s*id|game|id)\s*[:=]\s*([A-Za-z0-9_-]+)\b',
    caseSensitive: false,
  );
  static final _bareId = RegExp(r'^\s*([A-Za-z0-9_-]+)\s*$');

  static List<String> gameIds(String output) {
    final qualifiedIds = _qualifiedId
        .allMatches(output)
        .map((match) => match.group(1))
        .whereType<String>()
        .toSet()
        .toList();
    if (qualifiedIds.isNotEmpty) return qualifiedIds;

    final ids = <String>[];
    for (final line in output.split(RegExp(r'\r?\n'))) {
      final qualified = _qualifiedId.firstMatch(line)?.group(1);
      final labelled = _labelledId.firstMatch(line)?.group(1);
      final bare = _bareId.firstMatch(line)?.group(1);
      final id = qualified ?? labelled ?? bare;
      if (id != null && id.isNotEmpty && !ids.contains(id)) ids.add(id);
    }
    return ids;
  }

  static String detectedGameId(String output) {
    final ids = gameIds(output);
    if (ids.length != 1) {
      throw FormatException(
        ids.isEmpty
            ? 'ScummVM did not detect a game in this folder.'
            : 'ScummVM detected multiple games: ${ids.join(', ')}',
      );
    }
    return ids.single;
  }
}

class ScummVmPlanner {
  ScummVmPlanner._();

  static OperationPlan buildPlan(List<ScummVmGame> games) {
    final changes = <PlannedChange>[];
    final conflicts = <String>[];
    final reserved = <String>{};
    final sorted = [...games]
      ..sort((a, b) => a.folderPath.compareTo(b.folderPath));

    for (final game in sorted) {
      final target = game.launcherPath;
      if (game.targetAlreadyExists || !reserved.add(target)) {
        conflicts.add(
          'Skipped ${game.gameName}: target already exists: $target',
        );
        continue;
      }
      changes.add(
        PlannedChange(
          title: game.gameName,
          sourceFiles: [game.folderPath],
          targetFiles: [target],
          detailLabel: '.scummvm',
          detailPath: target,
          operations: [
            WriteTextFile(relativePath: target, contents: '${game.gameId}\n'),
          ],
          targetAlreadyExists: false,
        ),
      );
    }

    return OperationPlan(
      mode: ToolMode.scummVmLaunchers,
      changes: changes,
      operations: changes.expand((change) => change.operations).toList(),
      conflicts: conflicts,
    );
  }
}
