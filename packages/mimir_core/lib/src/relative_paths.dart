class RelativePaths {
  const RelativePaths._();

  static String parentOf(String path) {
    final index = path.lastIndexOf('/');
    return index < 0 ? '' : path.substring(0, index);
  }

  static String nameOf(String path) {
    final index = path.lastIndexOf('/');
    return index < 0 ? path : path.substring(index + 1);
  }

  static String join(String parent, String child) =>
      parent.trim().isEmpty ? child : '$parent/$child';

  static String normalize(String path) =>
      path.split('/').where((part) => part.isNotEmpty).join('/');
}
