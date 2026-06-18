package uk.gov.justice.hmpps.offenderevents.services.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson

class PrisonApiMockServer internal constructor() : WireMockServer(8086) {
  fun stubPrisonerDetails(
    offenderNumber: String?,
    legalStatus: String,
    recall: Boolean,
    lastMovementTypeCode: String,
    lastMovementReasonCode: String,
    status: String,
    latestLocationId: String,
  ) {
    stubFor(
      get(String.format("/api/offenders/%s", offenderNumber)).willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            prisonerDetails(
              offenderNumber,
              legalStatus,
              recall,
              lastMovementTypeCode,
              lastMovementReasonCode,
              status,
              latestLocationId,
            ),
          )
          .withStatus(200),
      ),
    )
  }

  fun stubPrisonerDetails404(offenderNumber: String?) {
    stubFor(
      get(String.format("/api/offenders/%s", offenderNumber)).willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(404),
      ),
    )
  }

  fun verifyPrisonerDetails404(offenderNumber: String?) {
    verify(getRequestedFor(WireMock.urlEqualTo(String.format("/api/offenders/%s", offenderNumber))))
  }

  fun stubBasicPrisonerDetails(offenderNumber: String, bookingId: Long?) {
    stubFor(
      get(String.format("/api/bookings/%d?basicInfo=true&extraInfo=false", bookingId)).willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(basicPrisonerDetails(offenderNumber, bookingId))
          .withStatus(200),
      ),
    )
  }

  fun stubPrisonerIdentifiers(mergedNumber: String, bookingId: Long?) {
    stubFor(
      get(String.format("/api/bookings/%d/identifiers?type=MERGED", bookingId)).willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mergeIdentifier(mergedNumber))
          .withStatus(200),
      ),
    )
  }

  fun stubGetMovementsByBooking(
    bookingId: Long?,
    response: String = """
  [
    {
      "sequence": 10,
      "fromAgency": "NHI",
      "toAgency": "SHEFCC",
      "movementType": "CRT",
      "directionCode": "OUT",
      "movementDateTime": "2023-02-10T08:11:00",
      "movementReasonCode": "PR",
      "createdDateTime": "2023-02-10T08:11:22.685037",
      "modifiedDateTime": "2023-02-10T14:19:14.560498"
    },
    {
      "sequence": 11,
      "fromAgency": "NHI",
      "toAgency": "SHEFCC",
      "movementType": "CRT",
      "directionCode": "OUT",
      "movementDateTime": "2023-02-10T08:11:00",
      "movementReasonCode": "PR",
      "createdDateTime": "2023-02-10T08:11:22.685037",
      "modifiedDateTime": "2023-02-10T14:19:14.560498"
    },
    {
      "sequence": 12,
      "fromAgency": "NHI",
      "toAgency": "SHEFCC",
      "movementType": "CRT",
      "directionCode": "OUT",
      "movementDateTime": "2023-02-10T08:11:00",
      "movementReasonCode": "PR",
      "createdDateTime": "2023-02-10T08:11:22.685037",
      "modifiedDateTime": "2023-02-10T14:19:14.560498"
    }
  ]
    """.trimIndent(),
  ) {
    stubFor(get(String.format("/api/movements/booking/%d", bookingId)).willReturn(okJson(response)))
  }

  private fun prisonerDetails(
    offenderNumber: String?,
    legalStatus: String,
    recall: Boolean,
    lastMovementTypeCode: String,
    lastMovementReasonCode: String,
    status: String,
    latestLocationId: String,
  ): String =
    """
            {
                "offenderNo": "$offenderNumber",
                "bookingId": 1201233,
                "bookingNo": "38559A",
                "offenderId": 2582162,
                "rootOffenderId": 2582162,
                "firstName": "ANDY",
                "lastName": "REMAND",
                "dateOfBirth": "1965-07-19",
                "age": 55,
                "activeFlag": true,
                "agencyId": "MDI",
                "assignedLivingUnitId": 4012,
                "alertsCodes": [],
                "activeAlertCount": 0,
                "inactiveAlertCount": 0,
                "alerts": [],
                "assignedLivingUnit": {
                    "agencyId": "MDI",
                    "locationId": 4012,
                    "description": "RECP",
                    "agencyName": "Moorland (HMP & YOI)"
                },
                "physicalAttributes": {
                    "sexCode": "M",
                    "gender": "Male",
                    "raceCode": "M2",
                    "ethnicity": "Mixed: White and Black African"
                },
                "physicalCharacteristics": [],
                "profileInformation": [
                    {
                        "type": "YOUTH",
                        "question": "Youth Offender?",
                        "resultValue": "No"
                    }
                ],
                "physicalMarks": [],
                "inOutStatus": "IN",
                "identifiers": [],
                "personalCareNeeds": [],
                "sentenceDetail": {
                    "bookingId": 1201233
                },
                "offenceHistory": [],
                "sentenceTerms": [],
                "aliases": [],
                "status": "$status",
                "statusReason": "$lastMovementTypeCode-$lastMovementReasonCode",
                "lastMovementTypeCode": "$lastMovementTypeCode",
                "lastMovementReasonCode": "$lastMovementReasonCode",
                "legalStatus": "$legalStatus",
                "recall": $recall,
                "imprisonmentStatus": "TRL",
                "imprisonmentStatusDescription": "Committed to Crown Court for Trial",
                "privilegeSummary": {
                    "bookingId": 1201233,
                    "iepLevel": "Standard",
                    "iepDate": "2021-06-01",
                    "iepTime": "2021-06-01T10:04:36",
                    "daysSinceReview": 0,
                    "iepDetails": []
                },
                "receptionDate": "2021-06-01",
                "locationDescription": "Moorland (HMP & YOI)",
                "latestLocationId": "$latestLocationId"
            }
                        
    """.trimIndent()

  private fun basicPrisonerDetails(offenderNumber: String, bookingId: Long?): String = String.format(
    """
            {
                "offenderNo": "%s",
                "bookingId": %d
            }
            
    """.trimIndent(),
    offenderNumber,
    bookingId,
  )

  private fun mergeIdentifier(mergedNumber: String): String = String.format(
    """
            [
                {
                    "type": "MERGE",
                    "value": "%s"
                }
            ]
            
    """.trimIndent(),
    mergedNumber,
  )

  fun stubHealthPing(status: Int) {
    val up = """
            {"status": "up"}
    """.trimIndent()
    val down = """
            {"status": "down"}
    """.trimIndent()
    stubFor(
      get("/health/ping").willReturn(
        WireMock.aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) up else down)
          .withStatus(status),
      ),
    )
  }
}
