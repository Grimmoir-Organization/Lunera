package org.grimmoir.lunera.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreensInitializationTest {
    @Test
    fun eachDestinationCanInitializeFirstInAFreshClassLoader() {
        val parent = javaClass.classLoader!!
        for ((destination, route) in listOf("Home" to "home", "Playlists" to "playlists", "Library" to "library", "Songs" to "songs", "Search" to "search_input", "Settings" to "settings")) {
            val isolated = object : ClassLoader(parent) {
                override fun loadClass(name: String, resolve: Boolean): Class<*> {
                    if (!name.startsWith("org.grimmoir.lunera.ui.screens.Screens")) return super.loadClass(name, resolve)
                    synchronized(this) {
                        val loaded = findLoadedClass(name) ?: parent.getResourceAsStream(name.replace('.', '/') + ".class")!!.use {
                            val bytes = it.readBytes()
                            defineClass(name, bytes, 0, bytes.size)
                        }
                        if (resolve) resolveClass(loaded)
                        return loaded
                    }
                }
            }
            val baseName = "org.grimmoir.lunera.ui.screens.Screens"
            val type = Class.forName(baseName + "$" + destination, true, isolated)
            val instance = type.getField("INSTANCE").get(null)
            assertEquals(route, type.getMethod("getRoute").invoke(instance))
            val base = Class.forName(baseName, true, isolated)
            val companion = base.getField("Companion").get(null)
            val mainScreens = companion.javaClass.getMethod("getMainScreens").invoke(companion) as List<*>
            assertEquals(listOf("songs", "playlists", "library", "search_input"), mainScreens.map { base.getMethod("getRoute").invoke(it) })
            assertEquals(setOf("songs", "playlists", "library", "search_input"), companion.javaClass.getMethod("getMainRoutes").invoke(companion))
        }
    }

    @Test
    fun destinationCanInitializeBeforeTheNavigationRegistry() {
        // This is the access order in MainActivityKt's top-level tab routes.
        // Eager companion collections capture Home.INSTANCE before assignment.
        assertEquals("home", Screens.Home.route)
        assertEquals(
            listOf("songs", "playlists", "library", "search_input"),
            Screens.MainScreens.map { it.route },
        )
        assertEquals(
            setOf("songs", "playlists", "library", "search_input"),
            Screens.MainRoutes,
        )
        assertEquals(
            listOf("songs", "playlists", "library", "search_input"),
            Screens.mainScreens(localOnly = true).map { it.route },
        )
    }
}
