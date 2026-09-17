package com.intelitec.fotostecnica.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta suave estilo Material 3 (tonos verdosos/marfileños)
private val PaletaSueve = lightColorScheme(
    primary = Color(0xFF4C7A5C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E8D8),
    onPrimaryContainer = Color(0xFF112818),
    secondary = Color(0xFF6B7A6E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0F5F1),
    onSecondaryContainer = Color(0xFF233327),
    tertiary = Color(0xFF8B7A5A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF1E7D3),
    onTertiaryContainer = Color(0xFF2A2112),
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFAF9F6),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFEAE8E1),
    onSurfaceVariant = Color(0xFF4C4A44),
    surfaceContainer = Color(0xFFF0EEEA),
    outline = Color(0xFF7D7B74)
)

@Composable
fun FotosTecnicaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PaletaSueve,
        typography = Typography(),
        content = content
    )
}