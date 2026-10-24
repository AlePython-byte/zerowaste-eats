package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.data.auth.FakeAuthRepository
import com.zerowasteeats.app.data.session.Session
import com.zerowasteeats.app.presentation.model.OfferCategory
import java.time.Clock
import java.time.Instant
import java.time.LocalTime

/** What a signed-in business works with. Only exists for a [Session.Merchant]. */
class MerchantRepositories(
    val offers: MerchantOfferRepository,
    val reservations: MerchantReservationRepository,
    val profile: MerchantProfileRepository
)

/**
 * Builds the fake repositories for a business. The demo account starts with sample offers and
 * reservations; a business that signed up in the app starts empty and unverified.
 */
object FakeMerchantData {

    fun repositoriesFor(
        session: Session.Merchant,
        latencyMillis: Long = FakeMerchantOfferRepository.DEFAULT_LATENCY_MILLIS,
        clock: Clock = Clock.systemDefaultZone()
    ): MerchantRepositories {
        val isDemo = session.email.equals(FakeAuthRepository.DEMO_MERCHANT_EMAIL, ignoreCase = true)
        return if (isDemo) {
            MerchantRepositories(
                offers = FakeMerchantOfferRepository(demoOffers(clock), latencyMillis, clock),
                reservations = FakeMerchantReservationRepository(
                    demoReservations(clock),
                    latencyMillis
                ),
                profile = FakeMerchantProfileRepository(
                    MerchantProfile(
                        businessName = session.businessName,
                        description = "Pan y repostería artesanal, hecho cada día.",
                        address = "Cra. 27 #18-45",
                        city = "Pasto",
                        isVerified = true
                    )
                )
            )
        } else {
            MerchantRepositories(
                offers = FakeMerchantOfferRepository(emptyList(), latencyMillis, clock),
                reservations = FakeMerchantReservationRepository(emptyList(), latencyMillis),
                profile = FakeMerchantProfileRepository(
                    MerchantProfile(
                        businessName = session.businessName,
                        description = "",
                        address = "",
                        // What was typed at sign up; the rest is filled in later
                        city = session.city,
                        isVerified = false
                    )
                )
            )
        }
    }

    /** Today at [hour]:[minute] in the clock's time zone. */
    private fun today(clock: Clock, hour: Int, minute: Int): Instant =
        clock.instant().atZone(clock.zone).toLocalDate()
            .atTime(LocalTime.of(hour, minute))
            .atZone(clock.zone)
            .toInstant()

    internal fun demoOffers(clock: Clock): List<MerchantOffer> = listOf(
        MerchantOffer(
            id = "demo-offer-1",
            title = "Croissants del día",
            category = OfferCategory.BAKERY,
            originalPrice = 15_000,
            discountedPrice = 8_900,
            quantity = 6,
            pickupStart = today(clock, 18, 0),
            pickupEnd = today(clock, 19, 30),
            status = MerchantOfferStatus.ACTIVE,
            photo = OfferPhoto.CROISSANT
        ),
        MerchantOffer(
            id = "demo-offer-2",
            title = "Caja de postres surtidos",
            category = OfferCategory.DESSERTS,
            originalPrice = 20_000,
            discountedPrice = 9_900,
            quantity = 3,
            pickupStart = today(clock, 17, 30),
            pickupEnd = today(clock, 19, 0),
            status = MerchantOfferStatus.ACTIVE,
            photo = OfferPhoto.DESSERTS
        ),
        MerchantOffer(
            id = "demo-offer-3",
            title = "Sándwich artesanal",
            category = OfferCategory.PREPARED_FOOD,
            originalPrice = 14_000,
            discountedPrice = 7_500,
            quantity = 4,
            pickupStart = today(clock, 18, 30),
            pickupEnd = today(clock, 20, 0),
            status = MerchantOfferStatus.PAUSED,
            photo = OfferPhoto.BURGER
        ),
        MerchantOffer(
            id = "demo-offer-4",
            title = "Caja de verduras frescas",
            category = OfferCategory.PRODUCE,
            originalPrice = 18_000,
            discountedPrice = 9_000,
            quantity = 0,
            pickupStart = today(clock, 16, 0),
            pickupEnd = today(clock, 18, 0),
            status = MerchantOfferStatus.SOLD_OUT,
            photo = OfferPhoto.VEGETABLES
        )
    )

    internal fun demoReservations(clock: Clock): List<MerchantReservation> = listOf(
        MerchantReservation(
            id = "demo-reservation-1",
            offerTitle = "Croissants del día",
            customerName = "Laura Gómez",
            quantity = 2,
            pickupTime = today(clock, 18, 15),
            code = "PAN-4821",
            status = MerchantReservationStatus.PENDING
        ),
        MerchantReservation(
            id = "demo-reservation-2",
            offerTitle = "Caja de postres surtidos",
            customerName = "Carlos Ruiz",
            quantity = 1,
            pickupTime = today(clock, 18, 45),
            code = "PAN-7305",
            status = MerchantReservationStatus.PENDING
        ),
        MerchantReservation(
            id = "demo-reservation-3",
            offerTitle = "Croissants del día",
            customerName = "Marta Díaz",
            quantity = 3,
            pickupTime = today(clock, 12, 30),
            code = "PAN-1190",
            status = MerchantReservationStatus.COMPLETED
        )
    )
}
