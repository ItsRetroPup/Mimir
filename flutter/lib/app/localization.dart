import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:xml/xml.dart';

class MimirStrings {
  MimirStrings(this.catalog);
  final Map<String, dynamic> catalog;
  static Map<String, dynamic> loaded = {};
  static Future<void> load() async {
    const folders = {'en': 'values', 'de': 'values-de', 'es': 'values-es', 'fr': 'values-fr',
      'it': 'values-it', 'ja': 'values-ja', 'ko': 'values-ko', 'pl': 'values-pl',
      'pt_BR': 'values-pt-rBR', 'zh_CN': 'values-zh-rCN', 'zh_TW': 'values-zh-rTW'};
    for (final locale in folders.entries) {
      final source = await rootBundle.loadString('../app/src/main/res/${locale.value}/strings.xml');
      loaded[locale.key] = {for (final node in XmlDocument.parse(source).findAllElements('string'))
        node.getAttribute('name')!: node.innerText};
    }
  }
  static String of(BuildContext context, String key, String fallback) {
    final locale = Localizations.localeOf(context);
    final language = loaded[locale.toString()] ?? loaded[locale.languageCode] ?? loaded['en'];
    return (language is Map ? language[key] as String? : null) ?? fallback;
  }
}
