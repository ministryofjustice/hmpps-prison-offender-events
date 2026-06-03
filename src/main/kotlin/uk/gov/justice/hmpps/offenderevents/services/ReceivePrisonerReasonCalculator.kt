package uk.gov.justice.hmpps.offenderevents.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.hmpps.offenderevents.services.MovementReason.TRANSFER

@Component
class ReceivePrisonerReasonCalculator(
  private val prisonApiService: PrisonApiService,
) {
  internal fun calculateMostLikelyReasonForPrisonerReceive(offenderNumber: String): ReceiveReason {
    val prisonerDetails = prisonApiService.getPrisonerDetails(offenderNumber)

    val reason = when (prisonerDetails.typeOfMovement()) {
      // They might not actually be in prison if they were received then went straight out on a TAP
      // or to court within the (usually 45 mins) grace period
      MovementType.TEMPORARY_ABSENCE -> if (prisonerDetails.currentLocation() == CurrentLocation.IN_PRISON) Reason.TEMPORARY_ABSENCE_RETURN else Reason.ADMISSION
      MovementType.COURT -> if (prisonerDetails.currentLocation() == CurrentLocation.IN_PRISON) Reason.RETURN_FROM_COURT else Reason.ADMISSION
      MovementType.ADMISSION -> when (prisonerDetails.movementReason()) {
        TRANSFER -> Reason.TRANSFERRED
        else -> Reason.ADMISSION
      }
      else -> Reason.ADMISSION
    }

    return ReceiveReason(
      reason = reason,
      details = "${prisonerDetails.status}:${prisonerDetails.statusReason}",
      currentLocation = prisonerDetails.currentLocation(),
      currentPrisonStatus = prisonerDetails.currentPrisonStatus(),
      prisonId = prisonerDetails.latestLocationId,
      nomisMovementReason = MovementReason(prisonerDetails.lastMovementReasonCode),
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
      if ((currentLocation == CurrentLocation.IN_PRISON) != (currentPrisonStatus == CurrentPrisonStatus.UNDER_PRISON_CARE)) {
        log.warn("hasPrisonerActuallyBeenReceived(): verdict based on active differs from old for $this")
      }
      return currentPrisonStatus == CurrentPrisonStatus.UNDER_PRISON_CARE
    }
  }

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
