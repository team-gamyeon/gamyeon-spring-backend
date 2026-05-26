package com.gamyeon.user.infrastructure.external;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

    private static final Logger log = LoggerFactory.getLogger(Provider.class);

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
      // ERROR: 설정 자체가 잘못된 경우 (env URIS가 비어 있거나 바인딩 실패)
      if (redirectUris.isEmpty()) {
        log.error(
            "[OAuth] redirectUris 목록이 비어 있습니다. "
                + "환경변수 OAUTH_*_REDIRECT_URIS가 올바르게 설정되었는지 확인하세요.");
        return null;
      }

      // DEBUG: 현재 로드된 redirect URI 목록 전체 출력
      log.debug("[OAuth] 등록된 redirectUris 목록 (총 {}개): {}", redirectUris.size(), redirectUris);

      // WARN: Origin 헤더가 없는 경우 (nginx가 헤더를 제거했거나, 비브라우저 클라이언트)
      if (origin == null || origin.isBlank()) {
        String fallback = redirectUris.get(0);
        log.warn("[OAuth] Origin 헤더가 없습니다. 첫 번째 URI로 폴백합니다. fallback={}", fallback);
        return fallback;
      }

      // DEBUG: 수신된 Origin 값과 매칭 시도 과정 출력
      log.debug("[OAuth] Origin 헤더 수신: '{}' — redirectUris에서 매칭 시도", origin);

      return redirectUris.stream()
          .filter(uri -> uri.startsWith(origin))
          .peek(
              matched ->
                  log.debug("[OAuth] 매칭 성공: origin='{}' → redirectUri='{}'", origin, matched))
          .findFirst()
          .orElseGet(
              () -> {
                String fallback = redirectUris.get(0);
                // WARN: origin이 있지만 매칭되는 URI가 없는 경우
                log.warn(
                    "[OAuth] origin='{}' 에 매칭되는 redirectUri가 없습니다. "
                        + "등록된 목록={}, 첫 번째 URI로 폴백합니다. fallback='{}'",
                    origin,
                    redirectUris,
                    fallback);
                return fallback;
              });
    }
  }
}
