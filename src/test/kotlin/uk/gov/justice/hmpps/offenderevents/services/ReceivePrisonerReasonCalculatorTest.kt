package uk.gov.justice.hmpps.offenderevents.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.hmpps.offenderevents.model.PrisonerReceivedOffenderEvent
import uk.gov.justice.hmpps.offenderevents.services.ReceivePrisonerReasonCalculator.Reason
import java.time.LocalDateTime

internal class ReceivePrisonerReasonCalculatorTest {
  private val prisonApiService: PrisonApiService = mock()
  private val calculator: ReceivePrisonerReasonCalculator = ReceivePrisonerReasonCalculator(prisonApiService)

  private val event = PrisonerReceivedOffenderEvent(
    eventDatetime = LocalDateTime.now(),
    offenderIdDisplay = "A1234GH",
    bookingId = 1234567,
    movementSeq = 1,
  )

  @Test
  @DisplayName("TAP movement type")
  fun tAPMovementTypeTakesPrecedenceOverRecall() {
    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "L")),
    )
    assertThat(calculator.calculateMostLikelyReasonForPrisonerReceive(event).reason).isEqualTo(Reason.ADMISSION)

    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("TAP"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "TAP", movementReasonCode = "C1")),
    )
    assertThat(calculator.calculateMostLikelyReasonForPrisonerReceive(event).reason).isEqualTo(Reason.TEMPORARY_ABSENCE_RETURN)
  }

  @Test
  @DisplayName("COURT movement type")
  fun courtMovementTypeTakesPrecedenceOverRecall() {
    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "V")),
    )
    assertThat(calculator.calculateMostLikelyReasonForPrisonerReceive(event).reason).isEqualTo(Reason.ADMISSION)

    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("CRT"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "CRT", movementReasonCode = "C1")),
    )
    assertThat(calculator.calculateMostLikelyReasonForPrisonerReceive(event).reason).isEqualTo(Reason.RETURN_FROM_COURT)
  }

  @Test
  @DisplayName("movement reason of INT means reason is a TRANSFER")
  fun movementReasonOfINTMeansReasonIsATRANSFER() {
    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM", "L"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "L", directionCode = "IN")),
    )
    with(calculator.calculateMostLikelyReasonForPrisonerReceive(event)) {
      assertThat(reason).isEqualTo(Reason.ADMISSION)
      assertThat(details).isEqualTo("ACTIVE IN:ADM-L")
      assertThat(currentLocation).isEqualTo(CurrentLocation.IN_PRISON)
      assertThat(currentPrisonStatus).isEqualTo(CurrentPrisonStatus.UNDER_PRISON_CARE)
      assertThat(prisonId).isEqualTo("MDI")
      assertThat(nomisMovementReason.code).isEqualTo("L")
    }

    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM", "INT"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "INT")),
    )
    with(calculator.calculateMostLikelyReasonForPrisonerReceive(event)) {
      assertThat(reason).isEqualTo(Reason.TRANSFERRED)
      assertThat(nomisMovementReason.code).isEqualTo("INT")
    }
  }

  @Test
  @DisplayName("movement reason of TRNCRT (transfer via court) means reason is a TRANSFER")
  fun movementReasonOfTRNCRTMeansReasonIsATRANSFER() {
    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "L")),
    )
    with(calculator.calculateMostLikelyReasonForPrisonerReceive(event)) {
      assertThat(reason).isEqualTo(Reason.ADMISSION)
      assertThat(nomisMovementReason.code).isEqualTo("L")
    }

    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "TRNCRT")),
    )
    with(calculator.calculateMostLikelyReasonForPrisonerReceive(event)) {
      assertThat(reason).isEqualTo(Reason.TRANSFERRED)
      assertThat(nomisMovementReason.code).isEqualTo("TRNCRT")
    }
  }

  @Test
  @DisplayName("movement reason of TRNTAP (transfer via TAP) means reason is a TRANSFER")
  fun movementReasonOfTRNTAPMeansReasonIsATRANSFER() {
    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "L")),
    )
    with(calculator.calculateMostLikelyReasonForPrisonerReceive(event)) {
      assertThat(reason).isEqualTo(Reason.ADMISSION)
      assertThat(nomisMovementReason.code).isEqualTo("L")
    }

    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(prisonerDetails("ADM"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "TRNTAP")),
    )
    with(calculator.calculateMostLikelyReasonForPrisonerReceive(event)) {
      assertThat(reason).isEqualTo(Reason.TRANSFERRED)
      assertThat(nomisMovementReason.code).isEqualTo("TRNTAP")
    }
  }

  @Test
  fun `when current status indicates in prison then they are really received`() {
    whenever(prisonApiService.getPrisonerDetails(any())).thenReturn(
      PrisonerDetails(
        "ADM",
        "L",
        "ACTIVE IN",
        "ADM-L",
        "MDI",
      ),
    )
    assertThat(calculator.calculateMostLikelyReasonForPrisonerReceive(event).hasPrisonerActuallyBeenReceived())
      .isTrue()
  }

  @Test
  fun `when current status indicates out on TAP they are still really received`() {
    whenever(prisonApiService.getPrisonerDetails(any()))
      .thenReturn(PrisonerDetails("TAP", "C6", "ACTIVE OUT", "TAP-C6", "MDI"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(
        BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "L", directionCode = "IN"),
        BookingMovement(sequence = 2, movementType = "TAP", movementReasonCode = "C6", directionCode = "OUT"),
      ),
    )
    with(calculator.calculateMostLikelyReasonForPrisonerReceive(event)) {
      assertThat(hasPrisonerActuallyBeenReceived()).isTrue()
      assertThat(reason).isEqualTo(Reason.ADMISSION)
      assertThat(details).isEqualTo("ACTIVE IN:ADM-L")
      assertThat(currentLocation).isEqualTo(CurrentLocation.OUTSIDE_PRISON)
      assertThat(currentPrisonStatus).isEqualTo(CurrentPrisonStatus.UNDER_PRISON_CARE)
      assertThat(prisonId).isEqualTo("MDI")
      assertThat(nomisMovementReason.code).isEqualTo("L")
    }
  }

  @Test
  fun `when they return from TAP but have gone out again immediately the reason is TAP`() {
    whenever(prisonApiService.getPrisonerDetails(any()))
      .thenReturn(PrisonerDetails("TAP", "C6", "ACTIVE OUT", "TAP-C6", "MDI"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(
        BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "L", directionCode = "IN"),
        BookingMovement(sequence = 2, movementType = "TAP", movementReasonCode = "C6", directionCode = "OUT"),
        BookingMovement(sequence = 3, movementType = "TAP", movementReasonCode = "C6", directionCode = "IN"),
        BookingMovement(sequence = 4, movementType = "TAP", movementReasonCode = "C7", directionCode = "OUT"),
      ),
    )
    with(
      calculator.calculateMostLikelyReasonForPrisonerReceive(
        PrisonerReceivedOffenderEvent(
          eventDatetime = LocalDateTime.now(),
          offenderIdDisplay = "A1234GH",
          bookingId = 1234567,
          movementSeq = 3,
        ),
      ),
    ) {
      assertThat(hasPrisonerActuallyBeenReceived()).isTrue()
      assertThat(reason).isEqualTo(Reason.TEMPORARY_ABSENCE_RETURN)
      assertThat(details).isEqualTo("ACTIVE IN:TAP-C6")
      assertThat(currentLocation).isEqualTo(CurrentLocation.OUTSIDE_PRISON)
      assertThat(currentPrisonStatus).isEqualTo(CurrentPrisonStatus.UNDER_PRISON_CARE)
      assertThat(prisonId).isEqualTo("MDI")
      assertThat(nomisMovementReason.code).isEqualTo("C6")
    }
  }

  @Test
  fun `when current status indicates out at court they are still really received`() {
    whenever(prisonApiService.getPrisonerDetails(any()))
      .thenReturn(PrisonerDetails("CRT", "DC", "ACTIVE OUT", "CRT-DC", "MDI"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(
        BookingMovement(sequence = 1, movementType = "ADM", movementReasonCode = "L", directionCode = "IN"),
        BookingMovement(sequence = 2, movementType = "CRT", movementReasonCode = "DC", directionCode = "OUT"),
      ),
    )
    val reason = calculator.calculateMostLikelyReasonForPrisonerReceive(event)
    assertThat(reason.hasPrisonerActuallyBeenReceived()).isTrue()
    assertThat(reason.reason).isEqualTo(Reason.ADMISSION)
  }

  @Test
  fun `when current status indicates not in prison then not really received`() {
    whenever(prisonApiService.getPrisonerDetails(any()))
      .thenReturn(PrisonerDetails("TRN", "P", "INACTIVE OUT", "TRN-P", "MDI"))
    whenever(prisonApiService.getMovementsByBooking(any())).thenReturn(
      listOf(
        BookingMovement(sequence = 1, movementType = "TRN", movementReasonCode = "P", directionCode = "OUT"),
      ),
    )

    assertThat(calculator.calculateMostLikelyReasonForPrisonerReceive(event).hasPrisonerActuallyBeenReceived())
      .isFalse()
  }

  private fun prisonerDetails(
    lastMovementTypeCode: String = "ADM",
    lastMovementReasonCode: String = "K",
  ): PrisonerDetails = PrisonerDetails(
    lastMovementTypeCode,
    lastMovementReasonCode,
    "ACTIVE IN",
    "$lastMovementTypeCode-$lastMovementReasonCode",
    "MDI",
  )
}
