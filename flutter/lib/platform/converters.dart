import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:path/path.dart' as p;
import '../domain/models.dart';
import 'storage.dart';

abstract interface class ConverterService {
  Future<String?> unavailableReason(Converter converter);
  Future<void> convert(FileOperation operation, String source, String output, String keys,
    Cancellation cancellation, void Function(double) progress);
}

/// Desktop release packaging places native tools beside the executable.
class BundledConverters implements ConverterService {
  BundledConverters({String? directory}) : directory = directory ?? p.join(p.dirname(Platform.resolvedExecutable), 'backends');
  final String directory;
  String executable(Converter converter) => p.join(directory,
    switch (converter) { Converter.chd => 'chdman', Converter.rvz => 'dolphintool', Converter.zcci => 'azahar', Converter.nsp => 'python/python' } + (Platform.isWindows ? '.exe' : ''));
  @override
  Future<String?> unavailableReason(Converter converter) async {
    if (Platform.isAndroid) {
      final available = await nativeChannel.invokeMethod<bool>('converterAvailable', {'tool': converter.name}) ?? false;
      return available ? null : '${converter.label} is missing from this build.';
    }
    if (!await File(executable(converter)).exists()) return '${converter.label} is missing from this build. Install a complete Mimir release.';
    if (converter == Converter.nsp && !await File(p.join(directory, 'mimir_nsz.py')).exists()) return 'The bundled NSZ adapter is missing.';
    return null;
  }
  @override
  Future<void> convert(FileOperation operation, String source, String output, String keys,
    Cancellation cancellation, void Function(double) progress) async {
    final converter = operation.converter!;
    final missing = await unavailableReason(converter);
    if (missing != null) throw StateError(missing);
    if (converter == Converter.nsp && !await File(keys).exists()) throw StateError('Import prod.keys before converting NSZ packages.');
    if (Platform.isAndroid) {
      final subscription = const EventChannel('pup.app.mimir/conversion').receiveBroadcastStream().listen((event) {
        if (event is num) progress(event.toDouble());
      });
      final timer = Timer.periodic(const Duration(milliseconds: 100), (_) {
        if (cancellation.immediate) { nativeChannel.invokeMethod<void>('cancelConversion'); }
      });
      try {
        await nativeChannel.invokeMethod<void>('convert', {'tool': converter.name, 'source': source,
          'output': output, 'keys': keys, 'discType': operation.discType.name});
        cancellation.check();
      } finally { timer.cancel(); await subscription.cancel(); }
      return;
    }
    final args = switch (converter) {
      Converter.chd => ['create${operation.discType.name}', '-i', source, '-o', output],
      Converter.rvz => ['convert', '-i', source, '-o', output, '-f', 'rvz', '-b', '131072', '-c', 'zstd', '-l', '5'],
      Converter.zcci => ['-c', source, '-o', p.dirname(output)],
      Converter.nsp => ['-I', p.join(directory, 'mimir_nsz.py'), source, output, keys],
    };
    final process = await Process.start(executable(converter), args, workingDirectory: p.dirname(source));
    final errors = StringBuffer();
    void collect(String line) {
      if (errors.length < 8192) errors.writeln(line);
      final match = RegExp(r'(\d{1,3}(?:\.\d+)?)\s*%').firstMatch(line);
      if (match != null) progress((double.parse(match[1]!) / 100).clamp(0, 1));
    }
    final stdout = process.stdout.transform(utf8.decoder).transform(const LineSplitter()).forEach(collect);
    final stderr = process.stderr.transform(utf8.decoder).transform(const LineSplitter()).forEach(collect);
    Timer? forceKill;
    final timer = Timer.periodic(const Duration(milliseconds: 100), (_) {
      if (cancellation.immediate && forceKill == null) {
        process.kill();
        forceKill = Timer(const Duration(seconds: 2), () => process.kill(ProcessSignal.sigkill));
      }
    });
    try {
      final exit = await process.exitCode;
      await stdout;
      await stderr;
      cancellation.check();
      if (exit != 0) throw StateError('${converter.label} failed (exit $exit). $errors');
      if (converter == Converter.zcci && !await File(output).exists()) {
        final generated = File(p.join(p.dirname(output), '${p.basenameWithoutExtension(source)}.zcci'));
        if (await generated.exists()) await generated.rename(output);
      }
      if (!await File(output).exists() || await File(output).length() == 0) throw StateError('${converter.label} did not produce an output.');
      if (converter == Converter.chd) {
        final verify = await Process.run(executable(converter), ['verify', '-i', output]);
        if (verify.exitCode != 0) throw StateError('CHD output failed verification; source files retained.');
      }
      progress(1);
    } finally { timer.cancel(); forceKill?.cancel(); if (cancellation.immediate) process.kill(ProcessSignal.sigkill); }
  }
}
