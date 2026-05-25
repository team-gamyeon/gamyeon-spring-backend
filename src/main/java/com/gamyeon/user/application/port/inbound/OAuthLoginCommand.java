package com.gamyeon.user.application.port.inbound;

import com.gamyeon.user.domain.OAuthProvider;

public class OAuthLoginCommand {

  private final OAuthProvider provider;
  private final String authorizationCode;
  private final String codeVerifier;
  private final String origin;

  private OAuthLoginCommand(OAuthProvider provider, String authorizationCode, String codeVerifier, String origin) {
    this.provider = provider;
    this.authorizationCode = authorizationCode;
    this.codeVerifier = codeVerifier;
    this.origin = origin;
  }

  public static OAuthLoginCommand of(
      OAuthProvider provider, String authorizationCode, String codeVerifier, String origin) {
    return new OAuthLoginCommand(provider, authorizationCode, codeVerifier, origin);
  }

  public OAuthProvider getProvider() {
    return provider;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public String getCodeVerifier() {
    return codeVerifier;
  }

  public String getOrigin() {
    return origin;
  }
}
