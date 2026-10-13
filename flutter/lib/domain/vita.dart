import 'dart:convert';
import 'dart:typed_data';
import 'models.dart';

Map<String, Object> parseSfo(Uint8List bytes) {
  if (bytes.length < 20) throw const FormatException('param.sfo is too small');
  final data = ByteData.sublistView(bytes);
  if (data.getUint32(0, Endian.little) != 0x46535000) throw const FormatException('Invalid param.sfo header');
  final keyStart = data.getUint32(8, Endian.little);
  final valueStart = data.getUint32(12, Endian.little);
  final count = data.getUint32(16, Endian.little);
  if (keyStart >= bytes.length || valueStart >= bytes.length || count > (bytes.length - 20) ~/ 16) {
    throw const FormatException('Invalid param.sfo tables');
  }
  final values = <String, Object>{};
  for (var i = 0; i < count; i++) {
    final offset = 20 + i * 16;
    final key = keyStart + data.getUint16(offset, Endian.little);
    final format = data.getUint16(offset + 2, Endian.little);
    final length = data.getUint32(offset + 4, Endian.little);
    final start = valueStart + data.getUint32(offset + 12, Endian.little);
    if (key >= bytes.length || start + length > bytes.length) throw const FormatException('Truncated param.sfo value');
    var end = key;
    while (end < bytes.length && bytes[end] != 0) { end++; }
    if (end == bytes.length) throw const FormatException('Unterminated param.sfo key');
    final name = utf8.decode(bytes.sublist(key, end));
    values[name] = switch (format) {
      0x0004 || 0x0204 => utf8.decode(bytes.sublist(start, start + length)).replaceAll(RegExp(r'\x00+$'), ''),
      0x0404 when length >= 4 => data.getUint32(start, Endian.little),
      _ => bytes.sublist(start, start + length),
    };
  }
  return values;
}

List<VitaApp> parseVitaDatabase(String text) => const LineSplitter().convert(text)
  .where((line) => line.trim().isNotEmpty)
  .map((line) => line.split('\t'))
  .where((fields) => fields.length >= 2 && RegExp(r'^[A-Z]{4}\d{5}$').hasMatch(fields[1]))
  .map((fields) => VitaApp(fields[1], fields[0])).toList();
