package com.zerowasteeats.app.presentation.navigation

enum class AppDestination(
    val route: String,
    val label: String
) {
    HOME(
        route = "home",
        label = "Inicio"
    ),
    EXPLORE(
        route = "explore",
        label = "Explorar"
    ),
    ORDERS(
        route = "orders",
        label = "Pedidos"
    ),
    PROFILE(
        route = "profile",
        label = "Perfil"
    )
}
