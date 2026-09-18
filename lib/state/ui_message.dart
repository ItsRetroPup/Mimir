import 'package:mimir_core/mimir_core.dart';

import '../l10n/app_localizations.dart';

enum UiMessageKind {
  raw,
  prodKeysImported,
  importKeysFailed,
  selectRomFolderFirst,
  importKeysFirst,
  toolDoesNotUseRomScanning,
  scanStopped,
  scanFailed,
  operationStopped,
  stoppedAfterOperations,
  changesApplied,
  applyFailed,
  stopping,
  stopAfterCurrent,
  selectVitaOutputFirst,
  unableToAddShortcut,
  addedShortcut,
  addShortcutFailed,
  removedShortcut,
  removeShortcutFailed,
  selectEsdeFolderFirst,
  selectRomRootForEsde,
  downloadingEsde,
  downloadedEsde,
  downloadEsdeFailed,
  installingEsde,
  installedEsde,
  installEsdeFailed,
  noMultiDiscGames,
  noZipCompatibleGames,
  noCompatibleConverterFiles,
  noVitaShortcutsQueued,
  noEsdeSystemsLoaded,
  scanMultiDisc,
  scanZipCompatible,
  scanConverter,
  scanFiles,
  scanFileCount,
  organizingProgress,
  zippingProgress,
  convertingProgress,
  creatingShortcutsProgress,
  updatingEsdeProgress,
}

class UiMessage {
  const UiMessage._(
    this.kind, {
    this.text,
    this.title,
    this.mode,
    this.converter,
    this.count,
    this.completed,
    this.total,
  });

  const UiMessage.raw(String text) : this._(UiMessageKind.raw, text: text);

  const UiMessage.prodKeysImported() : this._(UiMessageKind.prodKeysImported);

  const UiMessage.importKeysFailed() : this._(UiMessageKind.importKeysFailed);

  const UiMessage.selectRomFolderFirst()
    : this._(UiMessageKind.selectRomFolderFirst);

  const UiMessage.importKeysFirst() : this._(UiMessageKind.importKeysFirst);

  const UiMessage.toolDoesNotUseRomScanning()
    : this._(UiMessageKind.toolDoesNotUseRomScanning);

  const UiMessage.scanStopped() : this._(UiMessageKind.scanStopped);

  const UiMessage.scanFailed() : this._(UiMessageKind.scanFailed);

  const UiMessage.operationStopped() : this._(UiMessageKind.operationStopped);

  const UiMessage.stoppedAfterOperations(int completed, int total)
    : this._(
        UiMessageKind.stoppedAfterOperations,
        completed: completed,
        total: total,
      );

  const UiMessage.changesApplied() : this._(UiMessageKind.changesApplied);

  const UiMessage.applyFailed() : this._(UiMessageKind.applyFailed);

  const UiMessage.stopping() : this._(UiMessageKind.stopping);

  const UiMessage.stopAfterCurrent() : this._(UiMessageKind.stopAfterCurrent);

  const UiMessage.selectVitaOutputFirst()
    : this._(UiMessageKind.selectVitaOutputFirst);

  const UiMessage.unableToAddShortcut(String title)
    : this._(UiMessageKind.unableToAddShortcut, title: title);

  const UiMessage.addedShortcut(String title)
    : this._(UiMessageKind.addedShortcut, title: title);

  const UiMessage.addShortcutFailed() : this._(UiMessageKind.addShortcutFailed);

  const UiMessage.removedShortcut(String title)
    : this._(UiMessageKind.removedShortcut, title: title);

  const UiMessage.removeShortcutFailed()
    : this._(UiMessageKind.removeShortcutFailed);

  const UiMessage.selectEsdeFolderFirst()
    : this._(UiMessageKind.selectEsdeFolderFirst);

  const UiMessage.selectRomRootForEsde()
    : this._(UiMessageKind.selectRomRootForEsde);

  const UiMessage.downloadingEsde() : this._(UiMessageKind.downloadingEsde);

  const UiMessage.downloadedEsde(int count)
    : this._(UiMessageKind.downloadedEsde, count: count);

  const UiMessage.downloadEsdeFailed()
    : this._(UiMessageKind.downloadEsdeFailed);

  const UiMessage.installingEsde() : this._(UiMessageKind.installingEsde);

  const UiMessage.installedEsde(int count)
    : this._(UiMessageKind.installedEsde, count: count);

  const UiMessage.installEsdeFailed() : this._(UiMessageKind.installEsdeFailed);

  const UiMessage.noMultiDiscGames() : this._(UiMessageKind.noMultiDiscGames);

  const UiMessage.noZipCompatibleGames()
    : this._(UiMessageKind.noZipCompatibleGames);

  const UiMessage.noCompatibleConverterFiles(ConverterTool converter)
    : this._(UiMessageKind.noCompatibleConverterFiles, converter: converter);

  const UiMessage.noVitaShortcutsQueued()
    : this._(UiMessageKind.noVitaShortcutsQueued);

  const UiMessage.noEsdeSystemsLoaded()
    : this._(UiMessageKind.noEsdeSystemsLoaded);

  factory UiMessage.scanStarted(ToolMode mode, {ConverterTool? converter}) {
    final kind = switch (mode) {
      ToolMode.multiDiscOrganizer => UiMessageKind.scanMultiDisc,
      ToolMode.romZipper => UiMessageKind.scanZipCompatible,
      ToolMode.chdConverter => UiMessageKind.scanConverter,
      ToolMode.vitaAppIds || ToolMode.esDeSystems => UiMessageKind.scanFiles,
    };
    return UiMessage._(kind, mode: mode, converter: converter);
  }

  const UiMessage.scanFileCount(
    ToolMode mode,
    ConverterTool? converter,
    int count,
  ) : this._(
        UiMessageKind.scanFileCount,
        mode: mode,
        converter: converter,
        count: count,
      );

  factory UiMessage.applying(
    ToolMode mode,
    ConverterTool? converter,
    int completed,
    int total,
  ) {
    final kind = switch (mode) {
      ToolMode.multiDiscOrganizer => UiMessageKind.organizingProgress,
      ToolMode.romZipper => UiMessageKind.zippingProgress,
      ToolMode.chdConverter => UiMessageKind.convertingProgress,
      ToolMode.vitaAppIds => UiMessageKind.creatingShortcutsProgress,
      ToolMode.esDeSystems => UiMessageKind.updatingEsdeProgress,
    };
    return UiMessage._(
      kind,
      mode: mode,
      converter: converter,
      completed: completed,
      total: total,
    );
  }

  final UiMessageKind kind;
  final String? text;
  final String? title;
  final ToolMode? mode;
  final ConverterTool? converter;
  final int? count;
  final int? completed;
  final int? total;

  String resolve(AppLocalizations l10n) {
    final converterLabel = converter == null
        ? ''
        : localizedConverterLabel(l10n, converter!);
    return switch (kind) {
      UiMessageKind.raw => text ?? '',
      UiMessageKind.prodKeysImported => l10n.prodKeysImported,
      UiMessageKind.importKeysFailed => l10n.importKeysFailed,
      UiMessageKind.selectRomFolderFirst => l10n.selectRomFolderFirst,
      UiMessageKind.importKeysFirst => l10n.importKeysFirst,
      UiMessageKind.toolDoesNotUseRomScanning => l10n.toolDoesNotUseRomScanning,
      UiMessageKind.scanStopped => l10n.scanStopped,
      UiMessageKind.scanFailed => l10n.scanFailed,
      UiMessageKind.operationStopped => l10n.operationStopped,
      UiMessageKind.stoppedAfterOperations => l10n.stoppedAfterOperations(
        completed ?? 0,
        total ?? 0,
      ),
      UiMessageKind.changesApplied => l10n.changesApplied,
      UiMessageKind.applyFailed => l10n.applyFailed,
      UiMessageKind.stopping => l10n.stopping,
      UiMessageKind.stopAfterCurrent => l10n.stopAfterCurrentMessage,
      UiMessageKind.selectVitaOutputFirst => l10n.selectVitaOutputFirst,
      UiMessageKind.unableToAddShortcut => l10n.unableToAddShortcut(
        title ?? '',
      ),
      UiMessageKind.addedShortcut => l10n.addedShortcut(title ?? ''),
      UiMessageKind.addShortcutFailed => l10n.addShortcutFailed,
      UiMessageKind.removedShortcut => l10n.removedShortcut(title ?? ''),
      UiMessageKind.removeShortcutFailed => l10n.removeShortcutFailed,
      UiMessageKind.selectEsdeFolderFirst => l10n.selectEsdeFolderFirst,
      UiMessageKind.selectRomRootForEsde => l10n.selectRomRootForEsde,
      UiMessageKind.downloadingEsde => l10n.downloadingEsde,
      UiMessageKind.downloadedEsde => l10n.downloadedEsde(count ?? 0),
      UiMessageKind.downloadEsdeFailed => l10n.downloadEsdeFailed,
      UiMessageKind.installingEsde => l10n.installingEsde,
      UiMessageKind.installedEsde => l10n.installedEsde(count ?? 0),
      UiMessageKind.installEsdeFailed => l10n.installEsdeFailed,
      UiMessageKind.noMultiDiscGames => l10n.noMultiDiscGames,
      UiMessageKind.noZipCompatibleGames => l10n.noZipCompatibleGames,
      UiMessageKind.noCompatibleConverterFiles =>
        l10n.noCompatibleConverterFiles(converterLabel),
      UiMessageKind.noVitaShortcutsQueued => l10n.noVitaShortcutsQueued,
      UiMessageKind.noEsdeSystemsLoaded => l10n.noEsdeSystemsLoaded,
      UiMessageKind.scanMultiDisc => l10n.scanMultiDisc,
      UiMessageKind.scanZipCompatible => l10n.scanZipCompatible,
      UiMessageKind.scanConverter => l10n.scanConverter(converterLabel),
      UiMessageKind.scanFiles => l10n.scanFiles,
      UiMessageKind.scanFileCount => _scanFileCount(l10n, converterLabel),
      UiMessageKind.organizingProgress => l10n.organizingProgress(
        completed ?? 0,
        total ?? 0,
      ),
      UiMessageKind.zippingProgress => l10n.zippingProgress(
        completed ?? 0,
        total ?? 0,
      ),
      UiMessageKind.convertingProgress => l10n.convertingProgress(
        converterLabel,
        completed ?? 0,
        total ?? 0,
      ),
      UiMessageKind.creatingShortcutsProgress => l10n.creatingShortcutsProgress(
        completed ?? 0,
        total ?? 0,
      ),
      UiMessageKind.updatingEsdeProgress => l10n.updatingEsdeProgress(
        completed ?? 0,
        total ?? 0,
      ),
    };
  }

  String _scanFileCount(AppLocalizations l10n, String converterLabel) {
    final activity = switch (mode) {
      ToolMode.multiDiscOrganizer => l10n.scanMultiDisc.replaceFirst('…', ''),
      ToolMode.romZipper => l10n.scanZipCompatible.replaceFirst('…', ''),
      ToolMode.chdConverter =>
        l10n.scanConverter(converterLabel).replaceFirst('…', ''),
      ToolMode.vitaAppIds ||
      ToolMode.esDeSystems => l10n.scanFiles.replaceFirst('…', ''),
      null => l10n.scanFiles.replaceFirst('…', ''),
    };
    return '$activity: ${l10n.filesChecked(count ?? 0)}';
  }
}

String localizedConverterLabel(AppLocalizations l10n, ConverterTool tool) =>
    switch (tool) {
      ConverterTool.chd => l10n.toolChdman,
      ConverterTool.dolphinRvz => l10n.toolDolphinRvz,
      ConverterTool.azaharZcci => l10n.toolAzaharZcci,
      ConverterTool.nszNsp => l10n.toolNszToNsp,
    };

String localizedConverterDescription(
  AppLocalizations l10n,
  ConverterTool tool,
) => switch (tool) {
  ConverterTool.chd => l10n.toolChdmanDescription,
  ConverterTool.dolphinRvz => l10n.toolDolphinRvzDescription,
  ConverterTool.azaharZcci => l10n.toolAzaharZcciDescription,
  ConverterTool.nszNsp => l10n.toolNszToNspDescription,
};
