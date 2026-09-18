import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:ffi' show Abi;

import 'package:archive/archive.dart' hide ZipFile;
import 'package:file_selector/file_selector.dart';
import 'package:mimir_core/mimir_core.dart';
import 'package:path/path.dart' as p;

import 'esde_repository.dart';
import 'platform_adapter.dart';

class OperationStoppedException implements Exception {
  const OperationStoppedException([
    this.message = 'Operation stopped by user.',
  ]);

  final String message;

  @override
  String toString() => message;
}

class DesktopPlatformAdapter implements MimirPlatformAdapter {
  DesktopPlatformAdapter({this._resourceRoot, EsDeRepository? esDeRepository})
    : _esDeRepository = esDeRepository ?? EsDeRepository();

  final Directory? _resourceRoot;
  final EsDeRepository _esDeRepository;

  @override
  Future<String?> pickDirectory({
    required PickerPurpose purpose,
    String? initialHandle,
  }) {
    return getDirectoryPath(
      initialDirectory:
          initialHandle != null && Directory(initialHandle).existsSync()
          ? initialHandle
          : null,
      confirmButtonText: switch (purpose) {
        PickerPurpose.romRoot => 'Select ROM folder',
        PickerPurpose.vitaOutput => 'Select output folder',
        PickerPurpose.esdeRoot => 'Select ES-DE folder',
        PickerPurpose.esdeSystemFolder => 'Select system folder',
        PickerPurpose.nszKeys => 'Select folder',
      },
    );
  }

  @override
  Future<String?> pickFile({required PickerPurpose purpose}) async {
    final file = await openFile(
      acceptedTypeGroups: purpose == PickerPurpose.nszKeys
          ? const [
              XTypeGroup(label: 'Nintendo keys', extensions: ['keys']),
            ]
          : const [],
    );
    return file?.path;
  }

  @override
  Future<List<RomEntry>> scan({
    required String rootHandle,
    required ScanRequest request,
    bool scanHiddenFolders = false,
    void Function(int scannedFiles)? onProgress,
    CancellationToken? cancellation,
  }) async {
    final root = Directory(rootHandle);
    if (!await root.exists()) {
      throw StateError('Unable to access selected folder.');
    }
    final entries = <RomEntry>[];
    if (request.mode == ToolMode.multiDiscOrganizer ||
        request.mode == ToolMode.romZipper) {
      await _walk(
        node: root,
        sourceSegments: const [],
        outputSegments: const [],
        collector: entries,
        scanHiddenFolders: scanHiddenFolders,
        onProgress: onProgress,
        cancellation: cancellation,
      );
    } else {
      final converter = request.converterTool;
      final supported = converter == ConverterTool.chd
          ? request.chdSystem?.supportedExtensions ?? const <String>{}
          : converter?.sourceExtensions ?? const <String>{};
      final aliases = converter == ConverterTool.chd
          ? request.chdSystem?.folderAliases ?? const <String>{}
          : converter?.folderAliases ?? const <String>{};
      final outputExtension = converter == ConverterTool.chd
          ? 'chd'
          : converter?.outputExtension ?? '';
      await _scanConverterRoot(
        root: root,
        folderAliases: aliases,
        supportedExtensions: {
          ...supported,
          if (outputExtension.isNotEmpty) outputExtension,
        },
        collector: entries,
        onProgress: onProgress,
        cancellation: cancellation,
      );
    }
    return entries;
  }

  @override
  Future<StorageInfo?> storageInfo(String rootHandle) async => null;

  @override
  Future<void> importNszKeys({required String sourceHandle}) async {
    final source = File(sourceHandle);
    if (!await source.exists()) {
      throw StateError('The selected prod.keys file could not be read.');
    }
    if (await source.length() == 0) {
      throw StateError('The selected prod.keys file is empty.');
    }
    final destination = _nszKeysFile;
    await destination.parent.create(recursive: true);
    await source.copy(destination.path);
  }

  @override
  Future<bool> hasNszKeys() => _nszKeysFile.exists();

  @override
  Stream<OperationEvent> apply({
    required String rootHandle,
    required OperationPlan plan,
    CancellationToken? cancellation,
  }) {
    final controller = StreamController<OperationEvent>();
    final token = cancellation ?? CancellationToken();
    unawaited(_apply(rootHandle, plan, token, controller));
    return controller.stream;
  }

  Future<void> _apply(
    String rootHandle,
    OperationPlan plan,
    CancellationToken cancellation,
    StreamController<OperationEvent> controller,
  ) async {
    try {
      if (plan.operations.isEmpty) {
        throw StateError('No valid changes to apply.');
      }
      final root = Directory(rootHandle);
      var savedBytes = 0;
      for (var index = 0; index < plan.operations.length; index++) {
        if (cancellation.shouldStopBeforeNext) {
          controller.add(
            OperationEvent(
              completed: index,
              total: plan.operations.length,
              spaceSavedBytes: savedBytes,
              stopped: true,
              finished: true,
            ),
          );
          await controller.close();
          return;
        }
        final operation = plan.operations[index];
        controller.add(
          OperationEvent(
            completed: index,
            total: plan.operations.length,
            current: _operationLabel(operation),
            currentProgress: _isConversion(operation) ? 0 : null,
            spaceSavedBytes: savedBytes,
          ),
        );
        try {
          savedBytes += await _executeOperation(
            root,
            operation,
            cancellation,
            (progress) => controller.add(
              OperationEvent(
                completed: index,
                total: plan.operations.length,
                current: _operationLabel(operation),
                currentProgress: progress,
                spaceSavedBytes: savedBytes,
              ),
            ),
          );
        } on OperationStoppedException {
          controller.add(
            OperationEvent(
              completed: index,
              total: plan.operations.length,
              current: _operationLabel(operation),
              currentProgress: 0,
              spaceSavedBytes: savedBytes,
              stopped: true,
              finished: true,
            ),
          );
          await controller.close();
          return;
        }
        controller.add(
          OperationEvent(
            completed: index + 1,
            total: plan.operations.length,
            current: _operationLabel(operation),
            currentProgress: 1,
            spaceSavedBytes: savedBytes,
          ),
        );
      }
      controller.add(
        OperationEvent(
          completed: plan.operations.length,
          total: plan.operations.length,
          spaceSavedBytes: savedBytes,
          finished: true,
        ),
      );
      await controller.close();
    } catch (error, stackTrace) {
      controller.addError(error, stackTrace);
      await controller.close();
    }
  }

  Future<int> _executeOperation(
    Directory root,
    FileOperation operation,
    CancellationToken cancellation,
    void Function(double) onProgress,
  ) async {
    switch (operation) {
      case CreateDirectory(:final relativePath):
        await Directory(_resolve(root, relativePath)).create(recursive: true);
        return 0;
      case MoveFile(:final sourcePath, :final targetPath):
        final source = File(_resolve(root, sourcePath));
        final target = File(_resolve(root, targetPath));
        await target.parent.create(recursive: true);
        await _copyFile(source, target);
        await source.delete();
        return 0;
      case WriteTextFile(:final relativePath, :final contents):
        final target = File(_resolve(root, relativePath));
        await target.parent.create(recursive: true);
        await target.writeAsString(contents);
        return 0;
      case ZipFile(
        :final sourcePath,
        :final targetPath,
        :final archiveEntryName,
      ):
        final source = File(_resolve(root, sourcePath));
        final target = File(_resolve(root, targetPath));
        await target.parent.create(recursive: true);
        final bytes = await source.readAsBytes();
        final archive = Archive()
          ..addFile(ArchiveFile(archiveEntryName, bytes.length, bytes));
        final encoded = ZipEncoder().encode(archive);
        await target.writeAsBytes(encoded);
        await source.delete();
        return 0;
      case ConvertToChd(
        :final sourcePath,
        :final targetPath,
        :final discType,
        :final deleteOriginalFiles,
      ):
        final source = File(_resolve(root, sourcePath));
        final target = File(_resolve(root, targetPath));
        final companions = await _descriptorCompanions(source);
        final before = <File>[source, ...companions].fold<int>(
          0,
          (sum, file) => sum + (file.existsSync() ? file.lengthSync() : 0),
        );
        final temporary = await _runConverter(
          source: source,
          target: target,
          tool: ConverterTool.chd,
          discType: discType,
          cancellation: cancellation,
          onProgress: onProgress,
        );
        await target.parent.create(recursive: true);
        await _copyFile(temporary, target);
        await temporary.delete();
        if (deleteOriginalFiles) {
          for (final companion in [source, ...companions]) {
            if (await companion.exists()) await companion.delete();
          }
        }
        return before - await target.length();
      case ConvertWithTool(:final sourcePath, :final targetPath, :final tool):
        final source = File(_resolve(root, sourcePath));
        final target = File(_resolve(root, targetPath));
        final before = await source.length();
        final temporary = await _runConverter(
          source: source,
          target: target,
          tool: tool,
          cancellation: cancellation,
          onProgress: onProgress,
        );
        await target.parent.create(recursive: true);
        await _copyFile(temporary, target);
        await temporary.delete();
        return before - await target.length();
    }
  }

  Future<File> _runConverter({
    required File source,
    required File target,
    required ConverterTool tool,
    required CancellationToken cancellation,
    required void Function(double) onProgress,
    ChdDiscType? discType,
  }) async {
    final temporaryDirectory = await Directory.systemTemp.createTemp(
      'mimir-converter-',
    );
    final temporaryOutput = File(
      p.join(temporaryDirectory.path, p.basename(target.path)),
    );
    try {
      final executable = _executableFor(tool);
      final arguments = switch (tool) {
        ConverterTool.chd => [
          discType?.commandName ?? ChdDiscType.cd.commandName,
          '-i',
          source.path,
          '-o',
          temporaryOutput.path,
        ],
        ConverterTool.dolphinRvz => [
          'convert',
          '-i',
          source.path,
          '-o',
          temporaryOutput.path,
          '-f',
          'rvz',
          '-b',
          '131072',
          '-c',
          'zstd',
          '-l',
          '5',
        ],
        ConverterTool.azaharZcci => [
          '-c',
          source.path,
          '-o',
          temporaryDirectory.path,
        ],
        ConverterTool.nszNsp => [
          source.path,
          temporaryOutput.path,
          _nszKeysFile.path,
        ],
      };
      final process = await Process.start(
        executable.path,
        arguments,
        workingDirectory: temporaryDirectory.path,
      );
      final progressTimer = Timer.periodic(const Duration(milliseconds: 100), (
        _,
      ) {
        if (cancellation.shouldInterruptCurrent) process.kill();
      });
      final log = StringBuffer();
      Future<void> readOutput(Stream<List<int>> stream) async {
        await for (final line
            in stream.transform(utf8.decoder).transform(const LineSplitter())) {
          log.writeln(line);
          final match = RegExp(r'(\d{1,3}(?:\.\d+)?)\s*%').firstMatch(line);
          final percent = double.tryParse(match?.group(1) ?? '');
          if (percent != null) onProgress((percent / 100).clamp(0, 1));
        }
      }

      await Future.wait([
        readOutput(process.stdout),
        readOutput(process.stderr),
      ]);
      final exitCode = await process.exitCode;
      progressTimer.cancel();
      if (cancellation.shouldInterruptCurrent) {
        throw const OperationStoppedException();
      }
      if (exitCode != 0) {
        throw StateError(
          '${tool.displayName} failed to convert ${p.basename(source.path)}.\n$log',
        );
      }

      var produced = temporaryOutput;
      if (tool == ConverterTool.azaharZcci && !produced.existsSync()) {
        final candidates = temporaryDirectory
            .listSync()
            .whereType<File>()
            .where((file) => file.path.toLowerCase().endsWith('.zcci'))
            .toList();
        if (candidates.length == 1) produced = candidates.single;
      }
      if (!await produced.exists()) {
        throw StateError(
          '${tool.displayName} did not create ${p.basename(target.path)}.',
        );
      }
      onProgress(1);
      final retained = File(
        p.join(
          Directory.systemTemp.path,
          'mimir-produced-${DateTime.now().microsecondsSinceEpoch}-${p.basename(produced.path)}',
        ),
      );
      await produced.copy(retained.path);
      return retained;
    } catch (error) {
      if (cancellation.shouldInterruptCurrent) {
        throw const OperationStoppedException();
      }
      rethrow;
    } finally {
      if (await temporaryDirectory.exists()) {
        await temporaryDirectory.delete(recursive: true);
      }
    }
  }

  File _executableFor(ConverterTool tool) {
    final platformKey = switch (Platform.operatingSystem) {
      'windows' => 'windows-x64',
      'macos' => 'macos-${_isArm64 ? 'arm64' : 'x64'}',
      'linux' => 'linux-x64',
      _ => throw UnsupportedError(
        'Mimir converters are not available on ${Platform.operatingSystem}.',
      ),
    };
    final name = switch (tool) {
      ConverterTool.chd => Platform.isWindows ? 'chdman.exe' : 'chdman',
      ConverterTool.dolphinRvz =>
        Platform.isWindows ? 'dolphintool.exe' : 'dolphintool',
      ConverterTool.azaharZcci =>
        Platform.isWindows ? 'azahar-compress.exe' : 'azahar-compress',
      ConverterTool.nszNsp =>
        Platform.isWindows ? 'mimir-nsz.exe' : 'mimir-nsz',
    };
    final file = File(
      p.join(_resourceDirectory().path, 'bin', platformKey, name),
    );
    if (!file.existsSync()) {
      throw StateError(
        '${tool.displayName} is not bundled for ${Platform.operatingSystem}/$platformKey.',
      );
    }
    return file;
  }

  Directory _resourceDirectory() {
    if (_resourceRoot != null) return _resourceRoot;
    final candidates = [
      Directory(p.join(Directory.current.path, 'resources')),
      Directory(
        p.join(
          File(Platform.resolvedExecutable).parent.path,
          'data',
          'flutter_assets',
          'resources',
        ),
      ),
    ];
    return candidates.firstWhere(
      (directory) => directory.existsSync(),
      orElse: () => candidates.first,
    );
  }

  bool get _isArm64 => Abi.current() == Abi.macosArm64;

  File get _nszKeysFile {
    final base = switch (Platform.operatingSystem) {
      'windows' => Platform.environment['APPDATA'] ?? Directory.current.path,
      'macos' => p.join(
        Platform.environment['HOME'] ?? Directory.current.path,
        'Library',
        'Application Support',
      ),
      _ =>
        Platform.environment['XDG_CONFIG_HOME'] ??
            p.join(
              Platform.environment['HOME'] ?? Directory.current.path,
              '.config',
            ),
    };
    return File(p.join(base, 'Mimir', 'nsz', 'prod.keys'));
  }

  @override
  Future<void> deleteOutputFile({
    required String rootHandle,
    required String relativePath,
  }) async {
    final file = File(_resolve(Directory(rootHandle), relativePath));
    if (!await file.exists()) {
      throw StateError('Missing shortcut: $relativePath');
    }
    await file.delete();
  }

  @override
  Future<Map<String, String>> loadExistingVitaShortcuts({
    required String rootHandle,
    required VitaShortcutFormat format,
  }) async {
    final root = Directory(rootHandle);
    if (!await root.exists()) return {};
    final result = <String, String>{};
    await for (final entity in root.list(recursive: true, followLinks: false)) {
      if (entity is! File ||
          !entity.path.toLowerCase().endsWith('.${format.extension}')) {
        continue;
      }
      final relative = p
          .relative(entity.path, from: root.path)
          .replaceAll(p.separator, '/');
      final contents = await entity.readAsString();
      final titleId = format == VitaShortcutFormat.psvita
          ? contents.trim()
          : _parseDptTitleId(contents);
      if (titleId.isNotEmpty) result[titleId] = relative;
    }
    return result;
  }

  String _parseDptTitleId(String contents) {
    final lines = contents
        .split(RegExp(r'\r?\n'))
        .map((line) => line.trim())
        .toList();
    if (lines.isEmpty || lines.first != '[vita_game_id]') return '';
    return lines
        .skip(1)
        .firstWhere((line) => line.isNotEmpty, orElse: () => '');
  }

  @override
  Future<List<EsDeSystem>> loadInstalledEsDe({
    required String rootHandle,
    String? romRootHandle,
  }) => _esDeRepository.loadInstalled(rootHandle, romRootHandle: romRootHandle);

  @override
  Future<List<EsDeSystem>> fetchEsDeSystems({
    required String romRootHandle,
  }) async {
    final catalog = await _esDeRepository.fetchLatest();
    final folders = await _esDeRepository.scanRomFolders(romRootHandle);
    return _esDeRepository.systemsForRomFolders(catalog, folders);
  }

  @override
  Future<int> installEsDeSystems({
    required String esdeRootHandle,
    required String romRootHandle,
    required List<EsDeSystem> systems,
    required bool useLatestCatalog,
  }) {
    return useLatestCatalog
        ? _esDeRepository.installLatest(esdeRootHandle, systems)
        : _esDeRepository.installCurrent(esdeRootHandle, systems);
  }

  @override
  Future<String?> defaultEsDeSystemFolder({
    required String romRootHandle,
    required EsDeSystem system,
  }) async {
    if (!system.isDefault) return null;
    return p.join(romRootHandle, system.romFolder);
  }

  Future<void> _walk({
    required Directory node,
    required List<String> sourceSegments,
    required List<String> outputSegments,
    required List<RomEntry> collector,
    required bool scanHiddenFolders,
    void Function(int scannedFiles)? onProgress,
    CancellationToken? cancellation,
  }) async {
    _checkCancellation(cancellation);
    final children = await node.list(followLinks: false).toList();
    children.sort(
      (a, b) => p
          .basename(a.path)
          .toLowerCase()
          .compareTo(p.basename(b.path).toLowerCase()),
    );
    for (final child in children) {
      _checkCancellation(cancellation);
      final childName = p.basename(child.path);
      final type = await FileSystemEntity.type(child.path, followLinks: false);
      if (type == FileSystemEntityType.directory) {
        if (childName.toLowerCase().endsWith('.m3u')) continue;
        if (childName.startsWith('.') && !scanHiddenFolders) continue;
        await _walk(
          node: Directory(child.path),
          sourceSegments: [...sourceSegments, childName],
          outputSegments: childName.startsWith('.')
              ? outputSegments
              : [...outputSegments, childName],
          collector: collector,
          scanHiddenFolders: scanHiddenFolders,
          onProgress: onProgress,
          cancellation: cancellation,
        );
      } else if (type == FileSystemEntityType.file) {
        final relative = [...outputSegments, childName].join('/');
        final source = [...sourceSegments, childName].join('/');
        final size = await File(child.path).length();
        collector.add(
          RomEntry(
            relativePath: relative,
            fileName: childName,
            sourcePath: source,
            sizeBytes: size,
          ),
        );
        onProgress?.call(collector.length);
      }
    }
  }

  Future<void> _scanConverterRoot({
    required Directory root,
    required Set<String> folderAliases,
    required Set<String> supportedExtensions,
    required List<RomEntry> collector,
    void Function(int scannedFiles)? onProgress,
    CancellationToken? cancellation,
  }) async {
    final aliases = folderAliases.map((alias) => alias.toLowerCase()).toSet();
    final children = await root.list(followLinks: false).toList();
    children.sort(
      (a, b) => p
          .basename(a.path)
          .toLowerCase()
          .compareTo(p.basename(b.path).toLowerCase()),
    );
    for (final child in children) {
      _checkCancellation(cancellation);
      final name = p.basename(child.path);
      final type = await FileSystemEntity.type(child.path, followLinks: false);
      if (type == FileSystemEntityType.file &&
          supportedExtensions.contains(_extension(name))) {
        await _addConverterEntry(
          File(child.path),
          const [],
          const [],
          collector,
          onProgress,
        );
      } else if (type == FileSystemEntityType.directory &&
          aliases.contains(name.toLowerCase())) {
        await _walkConverter(
          Directory(child.path),
          [name],
          [name],
          supportedExtensions,
          collector,
          onProgress,
          cancellation,
        );
      }
    }
  }

  Future<void> _walkConverter(
    Directory node,
    List<String> sourceSegments,
    List<String> outputSegments,
    Set<String> supportedExtensions,
    List<RomEntry> collector,
    void Function(int scannedFiles)? onProgress,
    CancellationToken? cancellation,
  ) async {
    _checkCancellation(cancellation);
    final children = await node.list(followLinks: false).toList();
    children.sort(
      (a, b) => p
          .basename(a.path)
          .toLowerCase()
          .compareTo(p.basename(b.path).toLowerCase()),
    );
    for (final child in children) {
      _checkCancellation(cancellation);
      final name = p.basename(child.path);
      final type = await FileSystemEntity.type(child.path, followLinks: false);
      if (type == FileSystemEntityType.directory) {
        await _walkConverter(
          Directory(child.path),
          [...sourceSegments, name],
          [...outputSegments, name],
          supportedExtensions,
          collector,
          onProgress,
          cancellation,
        );
      } else if (type == FileSystemEntityType.file &&
          supportedExtensions.contains(_extension(name))) {
        await _addConverterEntry(
          File(child.path),
          sourceSegments,
          outputSegments,
          collector,
          onProgress,
        );
      }
    }
  }

  Future<void> _addConverterEntry(
    File file,
    List<String> sourceSegments,
    List<String> outputSegments,
    List<RomEntry> collector,
    void Function(int scannedFiles)? onProgress,
  ) async {
    final name = p.basename(file.path);
    final relative = [...outputSegments, name].join('/');
    final source = [...sourceSegments, name].join('/');
    final size = p.extension(name).toLowerCase() == '.cue'
        ? await _cueInputSize(file)
        : await file.length();
    collector.add(
      RomEntry(
        relativePath: relative,
        fileName: name,
        sourcePath: source,
        sizeBytes: size,
      ),
    );
    onProgress?.call(collector.length);
  }

  Future<int> _cueInputSize(File cue) async {
    final contents = await cue.readAsString();
    final parent = cue.parent;
    var total = 0;
    final pattern = RegExp(
      r'^\s*FILE\s+"([^"]+)"',
      caseSensitive: false,
      multiLine: true,
    );
    for (final match in pattern.allMatches(contents)) {
      final trackName = match.group(1);
      if (trackName == null || trackName.isEmpty) continue;
      final track = File(p.join(parent.path, trackName));
      if (await track.exists()) total += await track.length();
    }
    return total;
  }

  Future<List<File>> _descriptorCompanions(File source) async {
    final extension = p.extension(source.path).toLowerCase();
    if (extension != '.cue' && extension != '.gdi') return const [];
    final contents = await source.readAsString();
    final names = <String>{};
    if (extension == '.cue') {
      names.addAll(
        RegExp(r'^\s*FILE\s+"([^"]+)"', caseSensitive: false, multiLine: true)
            .allMatches(contents)
            .map((match) => match.group(1))
            .whereType<String>(),
      );
    } else {
      final pattern = RegExp(
        r'^\s*\d+\s+\d+\s+\d+\s+\d+\s+(?:"([^"]+)"|(\S+))',
        multiLine: true,
      );
      names.addAll(
        pattern
            .allMatches(contents)
            .map((match) => match.group(1) ?? match.group(2)!)
            .where((name) => name.isNotEmpty),
      );
    }
    return names
        .map((name) => File(p.join(source.parent.path, name)))
        .where((file) => file.existsSync())
        .toList();
  }

  Future<void> _copyFile(File source, File target) async {
    await source.openRead().pipe(target.openWrite());
  }

  String _resolve(Directory root, String relativePath) {
    if (p.isAbsolute(relativePath) || relativePath.contains('\\')) {
      throw ArgumentError('Invalid relative path: $relativePath');
    }
    final normalized = p.normalize(relativePath.replaceAll('/', p.separator));
    if (normalized == '..' || normalized.startsWith('..${p.separator}')) {
      throw ArgumentError('Invalid relative path: $relativePath');
    }
    return p.join(root.path, normalized);
  }

  String _extension(String name) {
    final extension = p.extension(name);
    return extension.isEmpty ? '' : extension.substring(1).toLowerCase();
  }

  String _operationLabel(FileOperation operation) => switch (operation) {
    CreateDirectory(:final relativePath) => relativePath,
    MoveFile(:final sourcePath) => sourcePath,
    WriteTextFile(:final relativePath) => relativePath,
    ZipFile(:final sourcePath) => sourcePath,
    ConvertToChd(:final sourcePath) => sourcePath,
    ConvertWithTool(:final sourcePath) => sourcePath,
  };

  bool _isConversion(FileOperation operation) =>
      operation is ConvertToChd || operation is ConvertWithTool;

  void _checkCancellation(CancellationToken? cancellation) {
    if (cancellation?.shouldInterruptCurrent == true) {
      throw const OperationStoppedException();
    }
  }
}
