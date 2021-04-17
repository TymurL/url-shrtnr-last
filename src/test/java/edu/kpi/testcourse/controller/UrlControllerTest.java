package edu.kpi.testcourse.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.kpi.testcourse.logic.PasswordEncoder;
import edu.kpi.testcourse.logic.PasswordEncoderImpl;
import edu.kpi.testcourse.model.UrlAlias;
import edu.kpi.testcourse.model.User;
import edu.kpi.testcourse.repository.UrlRepository;
import edu.kpi.testcourse.repository.UrlRepositoryImpl;
import edu.kpi.testcourse.repository.UserRepository;
import edu.kpi.testcourse.repository.UserRepositoryImpl;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.function.Executable;

@TestInstance(Lifecycle.PER_CLASS)
@MicronautTest
class UrlControllerTest {

  @Inject
  @Client("/")
  RxHttpClient client;

  @Inject
  UrlRepository urlRepository;

  @Inject
  UserRepository userRepository;

  @Inject
  PasswordEncoder passwordEncoder;

  String accessToken;

  @MockBean(UrlRepositoryImpl.class)
  UrlRepository urlRepo() {
    return mock(UrlRepository.class);
  }

  @MockBean(UserRepositoryImpl.class)
  UserRepository userRepo() {
    return mock(UserRepository.class);
  }

  @MockBean(PasswordEncoderImpl.class)
  PasswordEncoder passwordEncoder() {
    return mock(PasswordEncoder.class);
  }

  @BeforeAll
  void getAccessToken() {
    when(userRepository.getUserByEmail("test@mail.com"))
      .thenReturn(Optional.of(new User("test@mail.com", "123")));
    when(passwordEncoder.matches("123", "123")).thenReturn(true);

    HttpResponse<BearerAccessRefreshToken> response = client.toBlocking().exchange(
      HttpRequest.POST("/login", new UsernamePasswordCredentials("test@mail.com", "123")),
      BearerAccessRefreshToken.class
    );

    accessToken = Objects.requireNonNull(response.body()).getAccessToken();
  }

  @Test
  void shouldDeleteUrlSuccessfully() {
    when(urlRepository.getUserUrls("test@mail.com"))
      .thenReturn(
        List.of(
          new UrlAlias("a", "aaa", "test@mail.com"),
          new UrlAlias("b", "bbb", "test@mail.com")
        )
      );

    HttpResponse<String> response = client.toBlocking()
      .exchange(HttpRequest.DELETE("/urls/a").bearerAuth(accessToken), String.class);

    assertEquals(HttpStatus.NO_CONTENT, response.status());
    verify(urlRepository).remove("a");
  }

  @Test
  void shouldNotDeleteUrlBecauseItWasNotFound() {
    when(urlRepository.getUserUrls("test@mail.com"))
      .thenReturn(
        List.of(
          new UrlAlias("a", "aaa", "test@mail.com"),
          new UrlAlias("b", "bbb", "test@mail.com")
        )
      );

    Executable e = () -> client.toBlocking()
      .exchange(HttpRequest.DELETE("/urls/c").bearerAuth(accessToken), String.class);

    HttpClientResponseException thrown = assertThrows(HttpClientResponseException.class, e);
    assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
  }
}
