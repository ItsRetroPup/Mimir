import 'dart:typed_data';

class ParamSfoParser {
  ParamSfoParser._();

  static Map<String, Object> parse(List<int> bytes) {
    if (bytes.length < 20) throw FormatException('param.sfo is too small.');
    final data = Uint8List.fromList(bytes);
    final header = ByteData.sublistView(data);
    if (header.getUint32(0, Endian.little) != 0x46535000) {
      throw FormatException('Invalid param.sfo header.');
    }
    final keyTableStart = header.getUint32(8, Endian.little);
    final dataTableStart = header.getUint32(12, Endian.little);
    final entryCount = header.getUint32(16, Endian.little);
    if (keyTableStart >= data.length) {
      throw FormatException('Invalid key table offset.');
    }
    if (dataTableStart >= data.length) {
      throw FormatException('Invalid data table offset.');
    }

    final values = <String, Object>{};
    for (var index = 0; index < entryCount; index++) {
      final entryOffset = 20 + index * 16;
      if (entryOffset + 16 > data.length) {
        throw FormatException('Truncated param.sfo index table.');
      }
      final entry = ByteData.sublistView(data, entryOffset, entryOffset + 16);
      final keyOffset = entry.getUint16(0, Endian.little);
      final format = entry.getUint16(2, Endian.little);
      final dataLength = entry.getUint32(4, Endian.little);
      final dataOffset = entry.getUint32(12, Endian.little);
      final key = _readCString(data, keyTableStart + keyOffset);
      final valueStart = dataTableStart + dataOffset;
      if (valueStart >= data.length) {
        throw FormatException('Invalid value offset for key $key.');
      }
      if (valueStart + dataLength > data.length) {
        throw FormatException('Truncated value for key $key.');
      }
      final valueBytes = data.sublist(valueStart, valueStart + dataLength);
      values[key] = switch (format) {
        0x0004 ||
        0x0204 => String.fromCharCodes(valueBytes).replaceFirst('\u0000', ''),
        0x0404 when valueBytes.length >= 4 => ByteData.sublistView(
          valueBytes,
        ).getInt32(0, Endian.little),
        _ => valueBytes,
      };
    }
    return values;
  }

  static String _readCString(Uint8List bytes, int start) {
    if (start < 0 || start >= bytes.length) {
      throw FormatException('Invalid key offset.');
    }
    var end = start;
    while (end < bytes.length && bytes[end] != 0) {
      end++;
    }
    return String.fromCharCodes(bytes.sublist(start, end));
  }
}
