package com.zerowasteeats.app.data.merchant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferPhotoTest {

    @Test
    fun everyPhoto_survivesEncoding() {
        val photos = OfferPhoto.Stock.entries +
            OfferPhoto.Device("/data/user/0/app/cache/offer_photos/a b:c.jpg")

        photos.forEach { assertEquals(it, OfferPhoto.decode(OfferPhoto.encode(it))) }
    }

    @Test
    fun unknownText_decodesToNothing() {
        listOf("", "CROISSANT", "stock:PIZZA", "device:", "file:/x.jpg").forEach {
            assertNull(it, OfferPhoto.decode(it))
        }
    }
}
