sealed class ApiException implements Exception {
  const ApiException(this.message);
  final String message;

  @override
  String toString() => message;
}

class NetworkException extends ApiException {
  const NetworkException([super.message = 'İnternet bağlantısı yok veya sunucuya ulaşılamıyor.']);
}

class UnauthorizedException extends ApiException {
  const UnauthorizedException([super.message = 'Oturum süresi doldu. Lütfen tekrar giriş yapın.']);
}

class ForbiddenException extends ApiException {
  const ForbiddenException([super.message = 'Bu işlem için yetkiniz yok.']);
}

class NotFoundException extends ApiException {
  const NotFoundException([super.message = 'İstenen kaynak bulunamadı.']);
}

class ServerException extends ApiException {
  const ServerException([super.message = 'Sunucu hatası oluştu. Lütfen tekrar deneyin.']);
}

class UnknownException extends ApiException {
  const UnknownException([super.message = 'Beklenmeyen bir hata oluştu.']);
}
