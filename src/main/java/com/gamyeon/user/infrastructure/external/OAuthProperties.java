package com.gamyeon.user.infrastructure.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

  private Provider google = new Provider();
  private Provider kakao = new Provider();

  public Provider getGoogle() {
    return google;
  }

  public void setGoogle(Provider google) {
    this.google = google;
  }

  public Provider getKakao() {
    return kakao;
  }

  public void setKakao(Provider kakao) {
    this.kakao = kakao;
  }

  public static class Provider {
    private String clientId;
    private String clientSecret;
    private List<String> redirectUris = new ArrayList<>();

    public String getClientId() {
      return clientId;
    }

    public void setClientId(String clientId) {
      this.clientId = clientId;
    }

    public String getClientSecret() {
      return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
      this.clientSecret = clientSecret;
    }

    public List<String> getRedirectUris() {
      return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
      this.redirectUris = redirectUris;
    }

    public String resolveRedirectUri(String origin) {
      if (origin == null) return redirectUris.isEmpty() ? null : redirectUris.get(0);
      return redirectUris.stream()
          .filter(uri -> uri.startsWith(origin))
          .findFirst()
          .orElse(redirectUris.isEmpty() ? null : redirectUris.get(0));
    }
  }
}
