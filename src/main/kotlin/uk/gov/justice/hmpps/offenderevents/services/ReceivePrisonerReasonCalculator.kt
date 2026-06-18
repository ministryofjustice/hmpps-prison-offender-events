package uk.gov.justice.hmpps.offenderevents.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.offenderevents.model.PrisonerReceivedOffenderEvent
import uk.gov.justice.hmpps.offenderevents.services.CurrentLocation.IN_PRISON
import uk.gov.justice.hmpps.offenderevents.services.MovementReason.TRANSFER

@Component
class ReceivePrisonerReasonCalculator(
  private val prisonApiService: PrisonApiService,
) {
  internal fun calculateMostLikelyReasonForPrisonerReceive(event: PrisonerReceivedOffenderEvent): ReceiveReason {
    val prisonerDetails = prisonApiService.getPrisonerDetails(event.offenderIdDisplay)
    val movements = event.bookingId?.let { prisonApiService.getMovementsByBooking(it) }
    val receiveMovement = movements?.find { it.sequence == event.movementSeq }

    val reason = when (receiveMovement?.typeOfMovement()) {
      // They might not actually be in prison if they were received then went straight out on a TAP
      // or to court within the (usually 45 mins) grace period

      MovementType.TEMPORARY_ABSENCE -> Reason.TEMPORARY_ABSENCE_RETURN
      MovementType.COURT -> Reason.RETURN_FROM_COURT
      MovementType.ADMISSION -> when (receiveMovement.movementReason()) {
        TRANSFER -> Reason.TRANSFERRED
        else -> Reason.ADMISSION
      }
      else -> Reason.ADMISSION
    }
    val status = receiveMovement?.let { "ACTIVE ${receiveMovement.directionCode}" } ?: prisonerDetails.status
    val statusReason = receiveMovement?.let { "${receiveMovement.movementType}-${receiveMovement.movementReasonCode}" }
      ?: prisonerDetails.statusReason
    val nomisMovementReasonCode = receiveMovement?.movementReasonCode ?: prisonerDetails.lastMovementReasonCode

    return ReceiveReason(
      reason = reason,
      details = "$status:$statusReason",
      currentLocation = prisonerDetails.currentLocation(),
      currentPrisonStatus = prisonerDetails.currentPrisonStatus(),
      prisonId = prisonerDetails.latestLocationId,
      nomisMovementReason = MovementReason(nomisMovementReasonCode),
    )
  }

  enum class Reason {
    ADMISSION,
    TEMPORARY_ABSENCE_RETURN,
    RETURN_FROM_COURT,
    TRANSFERRED,
  }

  internal data class MovementReason(val code: String)

  internal data class ReceiveReason(
    val reason: Reason,
    override val details: String? = null,
    override val currentLocation: CurrentLocation?,
    override val currentPrisonStatus: CurrentPrisonStatus?,
    override val prisonId: String,
    val nomisMovementReason: MovementReason,
  ) : PrisonerMovementReason {
    fun hasPrisonerActuallyBeenReceived(): Boolean {
      if ((currentLocation == IN_PRISON) != (currentPrisonStatus == CurrentPrisonStatus.UNDER_PRISON_CARE)) {
        log.warn("hasPrisonerActuallyBeenReceived(): verdict based on active differs from old for $this")
      }
      return currentPrisonStatus == CurrentPrisonStatus.UNDER_PRISON_CARE
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
