package uk.gov.justice.hmpps.offenderevents.services

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.hmpps.offenderevents.services.PrisonerDetails.Companion.LICENCE_REVOKED
import uk.gov.justice.hmpps.offenderevents.services.PrisonerDetails.Companion.RECALL_FROM_DETENTION_TRAINING_ORDER
import uk.gov.justice.hmpps.offenderevents.services.PrisonerDetails.Companion.RECALL_FROM_HDC
import uk.gov.justice.hmpps.offenderevents.services.PrisonerDetails.Companion.TRANSFER_IN
import uk.gov.justice.hmpps.offenderevents.services.PrisonerDetails.Companion.TRANSFER_IN_VIA_COURT
import uk.gov.justice.hmpps.offenderevents.services.PrisonerDetails.Companion.TRANSFER_IN_VIA_TAP
import uk.gov.justice.hmpps.offenderevents.services.PrisonerDetails.Companion.UNCONVICTED_REMAND
import java.time.Duration
import java.time.LocalDateTime
import java.util.Optional

enum class MovementType {
  TEMPORARY_ABSENCE,
  COURT,
  ADMISSION,
  RELEASED,
  TRANSFER,
  OTHER,
}

enum class MovementReason {
  HOSPITALISATION,
  TRANSFER,
  RECALL,
  REMAND,
  OTHER,
}

@Service
class PrisonApiService(
  private val prisonApiWebClient: WebClient,
  @Value($$"${api.prisoner-timeout:30s}") private val timeout: Duration,
) {
  internal fun getPrisonerDetails(offenderNumber: String): PrisonerDetails = prisonApiWebClient.get()
    .uri("/api/offenders/{offenderNumber}", offenderNumber)
    .retrieve()
    .bodyToMono<PrisonerDetails>()
    .block(timeout)!!

  internal fun getPrisonerNumberForBookingId(bookingId: Long?): Optional<String> {
    val basicBookingDetail = prisonApiWebClient.get()
      .uri("/api/bookings/{bookingId}?basicInfo=true&extraInfo=false", bookingId)
      .retrieve()
      .bodyToMono<BasicBookingDetail>()
      .block(timeout)
    return if (basicBookingDetail != null) Optional.of(basicBookingDetail.offenderNo) else Optional.empty()
  }

  internal fun getIdentifiersByBookingId(bookingId: Long?): List<BookingIdentifier>? = prisonApiWebClient.get()
    .uri("/api/bookings/{bookingId}/identifiers?type=MERGED", bookingId)
    .retrieve()
    .bodyToMono<List<BookingIdentifier>>()
    .block(timeout)

  fun getMovementsByBooking(bookingId: Long): List<BookingMovement> = prisonApiWebClient.get()
    .uri("/api/movements/booking/{bookingId}", bookingId)
    .retrieve()
    .bodyToMono<List<BookingMovement>>()
    .block(timeout)!!
}

internal data class PrisonerDetails(
  val lastMovementTypeCode: String,
  val lastMovementReasonCode: String,
  val status: String?, // e.g. 'ACTIVE IN'
  val statusReason: String, // type-reason, e.g. TAP-OPA
  val latestLocationId: String, // prison e.g. SWI
) {
  fun typeOfMovement(): MovementType = when (lastMovementTypeCode) {
    "TAP" -> MovementType.TEMPORARY_ABSENCE
    "ADM" -> MovementType.ADMISSION
    "REL" -> MovementType.RELEASED
    "CRT" -> MovementType.COURT
    "TRN" -> MovementType.TRANSFER
    else -> MovementType.OTHER
  }

  fun movementReason(): MovementReason = when (lastMovementReasonCode) {
    "HP" -> MovementReason.HOSPITALISATION
    TRANSFER_IN, TRANSFER_IN_VIA_COURT, TRANSFER_IN_VIA_TAP -> MovementReason.TRANSFER
    LICENCE_REVOKED, RECALL_FROM_HDC, RECALL_FROM_DETENTION_TRAINING_ORDER -> MovementReason.RECALL
    UNCONVICTED_REMAND -> MovementReason.REMAND
    else -> MovementReason.OTHER
  }

  fun currentLocation(): CurrentLocation? = status?.let { secondOf(it) }
    ?.let {
      when (it) {
        "IN" -> CurrentLocation.IN_PRISON
        "OUT" -> CurrentLocation.OUTSIDE_PRISON
        "TRN" -> CurrentLocation.BEING_TRANSFERRED
        else -> null
      }
    }

  fun currentPrisonStatus(): CurrentPrisonStatus? = status?.let { firstOf(it) }
    ?.let {
      when (it) {
        "ACTIVE" -> CurrentPrisonStatus.UNDER_PRISON_CARE
        "INACTIVE" -> CurrentPrisonStatus.NOT_UNDER_PRISON_CARE
        else -> null
      }
    }

  private fun firstOf(value: String): String? = elementOf(value, 0)

  private fun secondOf(value: String): String? = elementOf(value, 1)

  private fun elementOf(value: String, index: Int): String? {
    val elements = value.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    return if (elements.size > index) {
      elements[index]
    } else {
      null
    }
  }

  companion object {
    const val UNCONVICTED_REMAND = "N"
    const val LICENCE_REVOKED = "L"
    const val RECALL_FROM_DETENTION_TRAINING_ORDER = "Y"
    const val RECALL_FROM_HDC = "B"
    const val TRANSFER_IN = "INT"
    const val TRANSFER_IN_VIA_COURT = "TRNCRT"
    const val TRANSFER_IN_VIA_TAP = "TRNTAP"
  }
}

internal data class BookingIdentifier(val value: String)
internal data class BasicBookingDetail(val offenderNo: String)

data class BookingMovement(
  @Schema(description = "Sequence number")
  val sequence: Int?,

  @Schema(description = "Agency travelling from")
  val fromAgency: String? = null,

  @Schema(description = "Agency travelling to")
  val toAgency: String? = null,

  @Schema(
    allowableValues = ["ADM", "CRT", "REL", "TAP", "TRN"],
  )
  val movementType: String? = null,

  @Schema(description = "IN or OUT")
  val directionCode: String? = null,

  @Schema(description = "Movement timestamp")
  val movementDateTime: LocalDateTime? = null,

  @Schema(description = "Code of movement reason")
  val movementReasonCode: String? = null,

  @Schema(description = "DB create timestamp")
  val createdDateTime: LocalDateTime? = null,

  @Schema(description = "DB modify timestamp")
  val modifiedDateTime: LocalDateTime? = null,
) {
  fun typeOfMovement(): MovementType = when (movementType) {
    "TAP" -> MovementType.TEMPORARY_ABSENCE
    "ADM" -> MovementType.ADMISSION
    "REL" -> MovementType.RELEASED
    "CRT" -> MovementType.COURT
    "TRN" -> MovementType.TRANSFER
    else -> MovementType.OTHER
  }

  fun movementReason(): MovementReason = when (movementReasonCode) {
    "HP" -> MovementReason.HOSPITALISATION
    TRANSFER_IN, TRANSFER_IN_VIA_COURT, TRANSFER_IN_VIA_TAP -> MovementReason.TRANSFER
    LICENCE_REVOKED, RECALL_FROM_HDC, RECALL_FROM_DETENTION_TRAINING_ORDER -> MovementReason.RECALL
    UNCONVICTED_REMAND -> MovementReason.REMAND
    else -> MovementReason.OTHER
  }
}
