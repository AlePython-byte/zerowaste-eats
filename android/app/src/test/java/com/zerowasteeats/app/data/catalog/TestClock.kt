package com.zerowasteeats.app.data.catalog

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock in UTC that a test moves by hand. */
class TestClock(var now: Instant = Instant.parse("2026-11-08T15:00:00Z")) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now
}
