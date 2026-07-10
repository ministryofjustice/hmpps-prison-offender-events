package uk.gov.justice.hmpps.offenderevents.smoketest

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.Builder
import uk.gov.justice.hmpps.kotlin.auth.HmppsWebClientConfiguration
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.oAuth2AuthorizedClientProvider
import java.time.Duration

@ConditionalOnProperty(name = ["smoketest.enabled"], havingValue = "true")
@EnableWebSecurity
@Import(OAuth2ClientAutoConfiguration::class, OAuth2ClientWebSecurityAutoConfiguration::class)
class SmokeTestConfiguration(@Value("\${smoketest.endpoint.url}") private val smokeTestUrl: String) {
  private val webClientBuilder: Builder = WebClient.builder()

  @Bean
  fun smokeTestWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient = webClientBuilder.authorisedWebClient(
    authorizedClientManager = authorizedClientManager,
    registrationId = "smoketest-service",
    url = smokeTestUrl,
    timeout = Duration.ofMinutes(5),
  )

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    oAuth2AuthorizedClientProvider: OAuth2AuthorizedClientProvider,
  ): OAuth2AuthorizedClientManager = HmppsWebClientConfiguration().authorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientProvider)

  @Bean
  fun authorizedClientProvider(): OAuth2AuthorizedClientProvider = oAuth2AuthorizedClientProvider(Duration.ofSeconds(30))
}
