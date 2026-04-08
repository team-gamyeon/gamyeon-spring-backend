package com.gamyeon.user.application.port.outbound;

public interface TokenPort {

  String createAccessToken(Long userId, String email);

  String createRefreshToken(Long userId);

  long getRefreshTokenExpiry();
}
