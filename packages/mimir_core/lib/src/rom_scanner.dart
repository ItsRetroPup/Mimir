import 'models.dart';
import 'relative_paths.dart';

class RomScanner {
  RomScanner._();

  static final List<RegExp> _patterns = [
    RegExp(r'\s*[\(\[]?(disc|disk|cd)\s*([0-9]+)[\)\]]?', caseSensitive: false),
    RegExp(
      r'\s*[\(\[]?([0-9]+)\s*(of)\s*([0-9]+)[\)\]]?',
      caseSensitive: false,
    ),
  ];

  static ScanResult scan(List<RomEntry> entries) {
    final grouped = <String, List<(RomEntry, DiscMatch)>>{};
    for (final entry in entries) {
      final discMatch = parseDisc(entry.fileName);
      if (discMatch == null) continue;
      final key =
          '${RelativePaths.parentOf(entry.relativePath)}\u0000${discMatch.title.toLowerCase()}';
      grouped.putIfAbsent(key, () => []).add((entry, discMatch));
    }

    final discSets = <DiscGameSet>[];
    for (final bucket in grouped.values) {
      if (bucket.length < 2) continue;
      bucket.sort((a, b) => a.$2.discNumber.compareTo(b.$2.discNumber));
      discSets.add(
        DiscGameSet(
          title: bucket.first.$2.title,
          parentPath: RelativePaths.parentOf(bucket.first.$1.relativePath),
          entries: bucket.map((item) => item.$1).toList(),
        ),
      );
    }
    discSets.sort(
      (a, b) => a.title.toLowerCase().compareTo(b.title.toLowerCase()),
    );
    return ScanResult(
      totalFiles: entries.length,
      allEntries: entries,
      discSets: discSets,
    );
  }

  static DiscMatch? parseDisc(String fileName) {
    final dot = fileName.lastIndexOf('.');
    final stem = dot < 0 ? fileName : fileName.substring(0, dot);
    for (var index = 0; index < _patterns.length; index++) {
      final match = _patterns[index].firstMatch(stem);
      if (match == null) continue;
      final discNumber = int.tryParse(
        index == 0 ? match.group(2)! : match.group(1)!,
      );
      if (discNumber == null) continue;
      var cleaned =
          '${stem.substring(0, match.start)}${stem.substring(match.end)}'
              .trim();
      cleaned = cleaned.replaceFirst(RegExp(r'[\s._-]+$'), '');
      cleaned = cleaned.replaceAll(RegExp(r'\s{2,}'), ' ');
      if (cleaned.trim().isEmpty) return null;
      return DiscMatch(title: cleaned, discNumber: discNumber);
    }
    return null;
  }
}
