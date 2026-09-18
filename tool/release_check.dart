import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:path/path.dart' as p;

const _androidTargets = <String, List<String>>{
  'android-arm64': [
    'android/app/src/main/jniLibs/arm64-v8a/libazahar.so',
    'android/app/src/main/jniLibs/arm64-v8a/libchdman.so',
    'android/app/src/main/jniLibs/arm64-v8a/libdolphintool.so',
    'android/app/src/main/python/mimir_nsz.py',
  ],
};

const _desktopExecutables = <String, List<String>>{
  'windows-x64': [
    'chdman.exe',
    'dolphintool.exe',
    'azahar-compress.exe',
    'mimir-nsz.exe',
  ],
  'macos-arm64': ['chdman', 'dolphintool', 'azahar-compress', 'mimir-nsz'],
  'macos-x64': ['chdman', 'dolphintool', 'azahar-compress', 'mimir-nsz'],
  'linux-x64': ['chdman', 'dolphintool', 'azahar-compress', 'mimir-nsz'],
};

const _requiredLicenses = <String>[
  'LICENSE',
  'assets/licenses/Azahar-GPL-2.0.txt',
  'assets/licenses/Dolphin-GPL-2.0.txt',
  'assets/licenses/MAME-GPL-2.0.txt',
  'assets/licenses/NSZ-MIT.txt',
];

Future<void> main(List<String> args) async {
  final target = _option(args, '--target');
  final writeHashes = args.contains('--write-hashes');
  final smoke = args.contains('--smoke');
  if (target == null ||
      !_androidTargets.containsKey(target) &&
          !_desktopExecutables.containsKey(target)) {
    stderr.writeln(
      'Usage: dart run tool/release_check.dart --target <android-arm64|windows-x64|macos-arm64|macos-x64|linux-x64> [--smoke] [--write-hashes]',
    );
    exitCode = 64;
    return;
  }

  final root = Directory.current;
  final failures = <String>[];
  final artifacts = <String>[];
  if (_androidTargets.containsKey(target)) {
    artifacts.addAll(_androidTargets[target]!);
  } else {
    artifacts.addAll(
      _desktopExecutables[target]!.map(
        (name) => p.join('resources', 'bin', target, name),
      ),
    );
  }
  artifacts.addAll(_requiredLicenses);

  final hashes = <String, String>{};
  for (final relativePath in artifacts) {
    final file = File(p.join(root.path, relativePath));
    if (!await file.exists()) {
      failures.add('Missing artifact: $relativePath');
      continue;
    }
    if (_isExecutableTarget(target) && relativePath.contains('/bin/')) {
      final mode = (await file.stat()).mode;
      if (Platform.isWindows) {
        if (!relativePath.toLowerCase().endsWith('.exe')) {
          failures.add(
            'Windows converter is missing .exe suffix: $relativePath',
          );
        }
      } else if (mode & 0x49 == 0) {
        failures.add('Converter is not executable: $relativePath');
      }
    }
    hashes[relativePath] = sha256.convert(await file.readAsBytes()).toString();
  }

  final manifest = File(p.join(root.path, 'resources', 'artifact-hashes.json'));
  if (await manifest.exists()) {
    final expected = (jsonDecode(await manifest.readAsString()) as Map).map(
      (key, value) => MapEntry(key.toString(), value.toString()),
    );
    for (final entry in expected.entries) {
      if (hashes.containsKey(entry.key) && hashes[entry.key] != entry.value) {
        failures.add('Hash mismatch: ${entry.key}');
      }
    }
  } else if (!writeHashes) {
    failures.add(
      'Missing resources/artifact-hashes.json; run with --write-hashes after reviewing artifacts.',
    );
  }

  if (smoke && failures.isEmpty && _desktopExecutables.containsKey(target)) {
    for (final relativePath in artifacts.where(
      (path) => path.contains('/bin/'),
    )) {
      final file = File(p.join(root.path, relativePath));
      try {
        final process = await Process.start(file.path, ['--help']);
        final output = Future.wait([
          process.stdout.drain(),
          process.stderr.drain(),
        ]);
        final exitCode = await process.exitCode.timeout(
          const Duration(seconds: 10),
          onTimeout: () {
            process.kill(ProcessSignal.sigterm);
            return -1;
          },
        );
        await output;
        if (exitCode < 0) {
          failures.add('Converter startup timed out: $relativePath');
        }
      } catch (error) {
        failures.add('Converter startup failed for $relativePath: $error');
      }
    }
  }

  if (target.startsWith('macos-')) await _checkMacSigning(root, failures);

  if (writeHashes &&
      failures
          .where((failure) => failure.startsWith('Missing artifact:'))
          .isEmpty) {
    await manifest.parent.create(recursive: true);
    await manifest.writeAsString(
      '${const JsonEncoder.withIndent('  ').convert(hashes)}\n',
    );
    stdout.writeln('Wrote ${p.relative(manifest.path, from: root.path)}');
  }

  if (failures.isNotEmpty) {
    stderr.writeln('Release check failed for $target:');
    for (final failure in failures) {
      stderr.writeln(' - $failure');
    }
    exitCode = 1;
    return;
  }
  stdout.writeln(
    'Release check passed for $target (${hashes.length} hashed files).',
  );
}

Future<void> _checkMacSigning(Directory root, List<String> failures) async {
  final app = Directory(
    p.join(
      root.path,
      'build',
      'macos',
      'Build',
      'Products',
      'Release',
      'mimir_flutter.app',
    ),
  );
  if (!await app.exists()) {
    failures.add(
      'Missing release macOS app for signing verification: ${p.relative(app.path, from: root.path)}',
    );
    return;
  }
  try {
    final result = await Process.run('codesign', [
      '--verify',
      '--deep',
      '--strict',
      app.path,
    ]);
    if (result.exitCode != 0) {
      failures.add('macOS codesign verification failed: ${result.stderr}');
    }
  } catch (error) {
    failures.add('Unable to run macOS codesign verification: $error');
  }
}

String? _option(List<String> args, String name) {
  final index = args.indexOf(name);
  if (index < 0 || index + 1 >= args.length) return null;
  return args[index + 1];
}

bool _isExecutableTarget(String target) => !_androidTargets.containsKey(target);
