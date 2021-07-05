package uk.gov.justice.hmpps.offenderevents.config

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext
import com.microsoft.applicationinsights.web.internal.ThreadContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.hmpps.offenderevents.helper.JwtAuthHelper
import java.time.Duration

@Import(JwtAuthHelper::class, ClientTrackingInterceptor::class, ClientTrackingConfiguration::class)
@ContextConfiguration(initializers = [ConfigDataApplicationContextInitializer::class])
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
class ClientTrackingConfigurationTest {
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var clientTrackingInterceptor: ClientTrackingInterceptor

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private lateinit var jwtAuthHelper: JwtAuthHelper
  private val res = MockHttpServletResponse()
  private val req = MockHttpServletRequest()

  @BeforeEach
  fun setup() {
    ThreadContext.setRequestTelemetryContext(RequestTelemetryContext(1L))
  }

  @AfterEach
  fun tearDown() {
    ThreadContext.remove()
  }

  @Test
  fun shouldAddClientIdAndUserNameToInsightTelemetry() {
    val token = jwtAuthHelper.createJwt("bob")
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    clientTrackingInterceptor.preHandle(req, res, "null")
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("username", "bob"), entry("clientId", "hmpps-offender-events-client"))
  }

  @Test
  fun shouldAddClientIdAndUserNameToInsightTelemetryEvenIfTokenExpired() {
    val token = jwtAuthHelper.createJwt("Fred", expiryTime = Duration.ofHours(-1L))
    req.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    clientTrackingInterceptor.preHandle(req, res, "null")
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("username", "Fred"), entry("clientId", "hmpps-offender-events-client"))
  }

  @Test
  fun shouldAddClientIpToInsightTelemetry() {
    val someIpAddress = "12.13.14.15"
    req.remoteAddr = someIpAddress
    clientTrackingInterceptor.preHandle(req, res, "null")
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("clientIpAddress", someIpAddress))
  }

  @Test
  fun shouldAddClientIpToInsightTelemetryWithoutPortNumber() {
    val someIpAddress = "12.13.14.15"
    req.remoteAddr = "$someIpAddress:6789"
    clientTrackingInterceptor.preHandle(req, res, "null")
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("clientIpAddress", someIpAddress))
  }
}
