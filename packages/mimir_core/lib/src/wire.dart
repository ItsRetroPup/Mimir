import 'models.dart';

const wireVersion = 1;

Map<String, Object?> operationToJson(FileOperation operation) =>
    switch (operation) {
      CreateDirectory(:final relativePath) => {
        'type': 'createDirectory',
        'relativePath': relativePath,
      },
      MoveFile(:final sourcePath, :final targetPath) => {
        'type': 'moveFile',
        'sourcePath': sourcePath,
        'targetPath': targetPath,
      },
      WriteTextFile(:final relativePath, :final contents) => {
        'type': 'writeTextFile',
        'relativePath': relativePath,
        'contents': contents,
      },
      ZipFile(:final sourcePath, :final targetPath, :final archiveEntryName) =>
        {
          'type': 'zipFile',
          'sourcePath': sourcePath,
          'targetPath': targetPath,
          'archiveEntryName': archiveEntryName,
        },
      ConvertToChd(
        :final sourcePath,
        :final targetPath,
        :final system,
        :final discType,
        :final deleteOriginalFiles,
      ) =>
        {
          'type': 'convertToChd',
          'sourcePath': sourcePath,
          'targetPath': targetPath,
          'system': system.name,
          'discType': discType.name,
          'deleteOriginalFiles': deleteOriginalFiles,
        },
      ConvertWithTool(:final sourcePath, :final targetPath, :final tool) => {
        'type': 'convertWithTool',
        'sourcePath': sourcePath,
        'targetPath': targetPath,
        'tool': tool.name,
      },
    };

FileOperation operationFromJson(Map<String, Object?> json) {
  final type = json['type'];
  return switch (type) {
    'createDirectory' => CreateDirectory(json['relativePath']! as String),
    'moveFile' => MoveFile(
      sourcePath: json['sourcePath']! as String,
      targetPath: json['targetPath']! as String,
    ),
    'writeTextFile' => WriteTextFile(
      relativePath: json['relativePath']! as String,
      contents: json['contents']! as String,
    ),
    'zipFile' => ZipFile(
      sourcePath: json['sourcePath']! as String,
      targetPath: json['targetPath']! as String,
      archiveEntryName: json['archiveEntryName']! as String,
    ),
    'convertToChd' => ConvertToChd(
      sourcePath: json['sourcePath']! as String,
      targetPath: json['targetPath']! as String,
      system: ChdSystem.values.byName(json['system']! as String),
      discType: ChdDiscType.values.byName(json['discType']! as String),
      deleteOriginalFiles: json['deleteOriginalFiles']! as bool,
    ),
    'convertWithTool' => ConvertWithTool(
      sourcePath: json['sourcePath']! as String,
      targetPath: json['targetPath']! as String,
      tool: ConverterTool.values.byName(json['tool']! as String),
    ),
    _ => throw FormatException('Unknown operation type: $type'),
  };
}

Map<String, Object?> planToJson(OperationPlan plan) => {
  'version': wireVersion,
  'mode': plan.mode.name,
  'preset': plan.preset?.name,
  'conflicts': plan.conflicts,
  'changes': plan.changes.map((change) {
    return {
      'title': change.title,
      'sourceFiles': change.sourceFiles,
      'targetFiles': change.targetFiles,
      'detailLabel': change.detailLabel,
      'detailPath': change.detailPath,
      'sourceSizeBytes': change.sourceSizeBytes,
      'targetAlreadyExists': change.targetAlreadyExists,
      'operations': change.operations.map(operationToJson).toList(),
    };
  }).toList(),
  'operations': plan.operations.map(operationToJson).toList(),
};

OperationPlan planFromJson(Map<String, Object?> json) {
  _checkVersion(json);
  final changes = ((json['changes']! as List).cast<Map>()).map((raw) {
    final change = raw.cast<String, Object?>();
    return PlannedChange(
      title: change['title']! as String,
      sourceFiles: (change['sourceFiles']! as List).cast<String>(),
      targetFiles: (change['targetFiles']! as List).cast<String>(),
      detailLabel: change['detailLabel']! as String,
      detailPath: change['detailPath']! as String,
      sourceSizeBytes: change['sourceSizeBytes']! as int,
      targetAlreadyExists: change['targetAlreadyExists']! as bool,
      operations: ((change['operations']! as List).cast<Map>())
          .map(
            (rawOperation) =>
                operationFromJson(rawOperation.cast<String, Object?>()),
          )
          .toList(),
    );
  }).toList();
  return OperationPlan(
    mode: ToolMode.values.byName(json['mode']! as String),
    preset: (json['preset'] as String?) == null
        ? null
        : FrontendPreset.values.byName(json['preset']! as String),
    changes: changes,
    operations: ((json['operations']! as List).cast<Map>())
        .map((raw) => operationFromJson(raw.cast<String, Object?>()))
        .toList(),
    conflicts: (json['conflicts']! as List).cast<String>(),
  );
}

Map<String, Object?> operationEventToJson(OperationEventData event) => {
  'version': wireVersion,
  'completed': event.completed,
  'total': event.total,
  'current': event.current,
  'currentProgress': event.currentProgress,
  'spaceSavedBytes': event.spaceSavedBytes,
  'stopped': event.stopped,
  'finished': event.finished,
  'error': event.error,
};

OperationEventData operationEventFromJson(Map<String, Object?> json) {
  _checkVersion(json);
  return OperationEventData(
    completed: _asInt(json['completed']),
    total: _asInt(json['total']),
    current: json['current'] as String?,
    currentProgress: _asDoubleOrNull(json['currentProgress']),
    spaceSavedBytes: _asInt(json['spaceSavedBytes']),
    stopped: json['stopped'] == true,
    finished: json['finished'] == true,
    error: json['error'] as String?,
  );
}

void _checkVersion(Map<String, Object?> json) {
  final version = json['version'];
  if (version != null && version != wireVersion) {
    throw FormatException('Unsupported Mimir wire version: $version');
  }
}

int _asInt(Object? value) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? 0;

double? _asDoubleOrNull(Object? value) =>
    value is num ? value.toDouble() : double.tryParse('$value');
