enum Tool { organiser, zipper, converters, vita, esde }

enum Frontend { esde, other }

enum Converter { chd, rvz, zcci, nsp }

enum DiscType { cd, dvd }

enum ShortcutFormat { psvita, dpt }

enum OperationKind { directory, move, text, zip, convert }

enum ItemStatus { completed, skipped, failed, cancelled }

class RomEntry {
  const RomEntry(this.path, {this.size = 0, this.isDirectory = false});
  final String path;
  final int size;
  final bool isDirectory;
  String get name => path.split('/').last;
  String get parent => path.contains('/') ? path.substring(0, path.lastIndexOf('/')) : '';
  String get extension => name.contains('.') ? name.split('.').last.toLowerCase() : '';
  String get stem => path.contains('.') ? path.substring(0, path.lastIndexOf('.')) : path;
}

class DiscMatch {
  const DiscMatch(this.title, this.number);
  final String title;
  final int number;
}

class VitaApp {
  const VitaApp(this.id, this.title, {this.source = 'shortcut-db'});
  final String id;
  final String title;
  final String source;
}

class FileOperation {
  const FileOperation(this.kind, this.target, {this.source, this.contents, this.converter,
    this.discType = DiscType.cd, this.deleteSource = false});
  final OperationKind kind;
  final String target;
  final String? source;
  final String? contents;
  final Converter? converter;
  final DiscType discType;
  final bool deleteSource;
}

class PlannedChange {
  const PlannedChange(this.title, this.operations, {this.sourceSize = 0, this.targetExists = false});
  final String title;
  final List<FileOperation> operations;
  final int sourceSize;
  final bool targetExists;
  List<String> get targets => operations.where((o) => o.kind != OperationKind.directory).map((o) => o.target).toList();
}

class OperationPlan {
  const OperationPlan(this.changes, {this.conflicts = const []});
  final List<PlannedChange> changes;
  final List<String> conflicts;
}

class ItemResult {
  const ItemResult(this.title, this.status, [this.message = '']);
  final String title;
  final ItemStatus status;
  final String message;
}

class Cancellation {
  bool immediate = false;
  bool afterCurrent = false;
  bool get requested => immediate || afterCurrent;
  void check() { if (immediate) throw const OperationCancelled(); }
}

class OperationCancelled implements Exception {
  const OperationCancelled();
  @override
  String toString() => 'Stopped by user';
}

String joinRelative(String parent, String name) => parent.isEmpty ? name : '$parent/$name';

const zipExtensions = {
  'gb', 'gbc', 'gba', 'nds', 'fds', 'fig', 'nes', 'sfc', 'smc', 'swc',
  'n64', 'v64', 'z64', 'a26', 'a52', 'a78', 'lnx', '32x', 'gen', 'md',
  'smd', 'gg', 'sc', 'sg', 'sms', 'ngc', 'ngp', 'pce', 'sgx', 'ws', 'wsc',
  'col', 'cv', 'd64', 'mx1', 'mx2', 'rom', 'sna', 'tap', 'tzx', 'z80',
};

const chdSystems = <String, ({Set<String> extensions, Set<String> aliases})>{
  'Dreamcast': (extensions: {'gdi', 'cue', 'iso'}, aliases: {'dreamcast', 'dc'}),
  'PlayStation 1': (extensions: {'cue', 'iso'}, aliases: {'playstation 1', 'ps1', 'psx'}),
  'PlayStation 2': (extensions: {'iso'}, aliases: {'playstation 2', 'ps2'}),
  'Sega CD': (extensions: {'cue', 'iso'}, aliases: {'sega cd', 'segacd', 'mega cd', 'megacd'}),
  'Sega Saturn': (extensions: {'cue', 'iso'}, aliases: {'sega saturn', 'saturn'}),
  'PlayStation Portable': (extensions: {'iso'}, aliases: {'playstation portable', 'psp'}),
};

extension ConverterInfo on Converter {
  String get label => switch (this) {
    Converter.chd => 'CHDMan', Converter.rvz => 'Dolphin RVZ',
    Converter.zcci => 'Azahar ZCCI', Converter.nsp => 'NSZ to NSP',
  };
  String get extension => name;
  Set<String> get extensions => switch (this) {
    Converter.chd => {'cue', 'gdi', 'iso'}, Converter.rvz => {'iso'},
    Converter.zcci => {'3ds', 'cci'}, Converter.nsp => {'nsz'},
  };
  Set<String> get aliases => switch (this) {
    Converter.chd => {}, Converter.rvz => {'gamecube', 'gc', 'nintendo gamecube', 'wii', 'nintendo wii'},
    Converter.zcci => {'3ds', 'nintendo 3ds'}, Converter.nsp => {'switch', 'nintendo switch'},
  };
}
