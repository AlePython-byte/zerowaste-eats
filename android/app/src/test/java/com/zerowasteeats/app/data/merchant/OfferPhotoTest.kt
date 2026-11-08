package com.zerowasteeats.app.data.merchant

import com.zerowasteeats.app.data.photos.LocalOfferPhotoUploader
import com.zerowasteeats.app.data.photos.OfferPhotoUploader
import com.zerowasteeats.app.data.photos.PhotoUpload
import com.zerowasteeats.app.data.photos.PhotoUploadError
import com.zerowasteeats.app.presentation.screens.merchant.editor.PhotoPublish
import com.zerowasteeats.app.presentation.screens.merchant.editor.publishPhoto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferPhotoTest {

    private val picked = PickedPhoto("/data/user/0/app/cache/offer_photos/a b:c.jpg")
    private val remote = OfferPhoto.Remote("https://ref.supabase.co/storage/v1/object/public/offer-photos/u/x.jpg?v=1")

    @Test
    fun everyKindOfPhoto_survivesEncoding() {
        val photos: List<PhotoSelection> = OfferPhoto.Bundled.entries + picked + remote

        photos.forEach { assertEquals(it, PhotoSelection.decode(PhotoSelection.encode(it))) }
    }

    @Test
    fun unknownText_decodesToNothing() {
        listOf("", "CROISSANT", "bundled:PIZZA", "remote:", "picked:", "file:/x.jpg").forEach {
            assertNull(it, PhotoSelection.decode(it))
        }
    }

    @Test
    fun aPickedPhoto_hasAFileUrl() {
        assertEquals("file:///data/x.jpg", PickedPhoto("/data/x.jpg").fileUrl)
    }

    @Test
    fun theDemoUploader_keepsThePhotoOnTheDevice() = runBlocking {
        assertEquals(
            PhotoUpload.Uploaded(OfferPhoto.Remote("file://${picked.path}")),
            LocalOfferPhotoUploader().upload(picked)
        )
    }

    // --- Publishing a chosen photo before saving ---

    private val neverCalled = OfferPhotoUploader { error("an already published photo is not uploaded") }

    @Test
    fun anAlreadyPublishedPhoto_isUsedAsItIs() = runBlocking {
        assertEquals(PhotoPublish.Ready(remote), publishPhoto(remote, neverCalled))
        assertEquals(
            PhotoPublish.Ready(OfferPhoto.Bundled.DESSERTS),
            publishPhoto(OfferPhoto.Bundled.DESSERTS, neverCalled)
        )
    }

    @Test
    fun aPickedPhoto_isUploaded_andTheResultIsUsed() = runBlocking {
        val uploaded = mutableListOf<PickedPhoto>()
        val uploader = OfferPhotoUploader {
            uploaded += it
            PhotoUpload.Uploaded(remote)
        }

        assertEquals(PhotoPublish.Ready(remote), publishPhoto(picked, uploader))
        assertEquals(listOf(picked), uploaded)
    }

    @Test
    fun aFailedUpload_isReported_withItsReason() = runBlocking {
        val result = publishPhoto(picked) { PhotoUpload.Failed(PhotoUploadError.NETWORK) }

        assertTrue(result is PhotoPublish.Failed)
        assertEquals(PhotoUploadError.NETWORK, (result as PhotoPublish.Failed).error)
    }
}
