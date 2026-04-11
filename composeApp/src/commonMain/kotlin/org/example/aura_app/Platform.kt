package org.example.aura_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform