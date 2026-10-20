package dev.anantpandey.shunya.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The resolver is the one piece of logic the whole launcher stands on, and
 * every case here is a way typing used to dead-end on a real phone.
 */
class ResolutionTest {

    private val phone = AppEntry("com.android.dialer", "Phone", Tier.ZERO)
    private val phoneLink = AppEntry("com.microsoft.appmanager", "Phone Link", Tier.ONE)
    private val googleAuth = AppEntry("com.google.android.apps.authenticator2", "Authenticator", Tier.ONE)
    private val msAuth = AppEntry("com.azure.authenticator", "Authenticator", Tier.ONE)
    private val calendar = AppEntry("com.google.android.calendar", "Calendar", Tier.ONE)
    private val calculator = AppEntry("com.google.android.calculator", "Calculator", Tier.ONE)
    private val x = AppEntry("com.twitter.android", "X", Tier.ONE)
    private val instagram = AppEntry("com.instagram.android", "Instagram", Tier.TWO)

    private val googleApps = listOf("Google", "Google Maps", "Google Pay", "Google Photos", "Google Drive")
        .mapIndexed { i, label -> AppEntry("com.google.app$i", label, Tier.ONE) }

    private val apps =
        listOf(phone, phoneLink, googleAuth, msAuth, calendar, calculator, x, instagram) + googleApps

    private fun matches(query: String) = apps.resolve(query) as Resolution.Matches

    @Test
    fun `a complete name wins over apps that share its prefix`() {
        val r = matches("phone")
        assertEquals(phone, r.primary)
        assertEquals(listOf(phone, phoneLink), r.shown)
    }

    @Test
    fun `two apps with the same name are both shown and neither is primary`() {
        val r = matches("authenticator")
        assertNull(r.primary)
        assertEquals(setOf(googleAuth, msAuth), r.shown.toSet())
    }

    @Test
    fun `same-name apps are listed from the first keystroke that matches`() {
        val r = matches("auth")
        assertEquals(setOf(googleAuth, msAuth), r.shown.toSet())
        assertNull(r.primary)
    }

    @Test
    fun `an open prefix shows the candidates without a primary`() {
        val r = matches("cal")
        assertEquals(listOf(calculator, calendar), r.shown)
        assertNull(r.primary)
        assertEquals(0, r.hidden)
    }

    @Test
    fun `a sole prefix match is primary`() {
        assertEquals(calendar, matches("cale").primary)
    }

    @Test
    fun `short queries need an exact match`() {
        assertEquals(Resolution.Nothing, apps.resolve("ph"))
        assertEquals(x, matches("x").primary)
    }

    @Test
    fun `more than three matches are counted not listed`() {
        val r = matches("goo")
        assertEquals(3, r.shown.size)
        assertEquals(2, r.hidden)
        assertNull(r.primary)
    }

    @Test
    fun `an exact match stays visible and primary when its prefix is crowded`() {
        val google = googleApps.first()
        val r = matches("google")
        assertEquals(google, r.primary)
        assertEquals(google, r.shown.first())
        assertEquals(2, r.hidden)
    }

    @Test
    fun `tier two needs the whole name and never appears by prefix`() {
        assertEquals(Resolution.Nothing, apps.resolve("instagra"))
        assertEquals(Resolution.Gated(instagram), apps.resolve("instagram"))
    }

    @Test
    fun `spaces and case are ignored`() {
        assertEquals(phoneLink, matches("Phone Link").primary)
    }

    @Test
    fun `nothing matches nothing`() {
        assertEquals(Resolution.Nothing, apps.resolve(""))
        assertEquals(Resolution.Nothing, apps.resolve("zzz"))
    }

    @Test
    fun `hint is the publisher segment of the package`() {
        assertEquals("google", googleAuth.hint)
        assertEquals("azure", msAuth.hint)
        assertEquals("microsoft", phoneLink.hint)
    }
}
