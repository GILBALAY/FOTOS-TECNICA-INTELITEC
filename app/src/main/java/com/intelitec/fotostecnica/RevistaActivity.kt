package com.intelitec.fotostecnica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intelitec.fotostecnica.data.Foto
import com.intelitec.fotostecnica.data.FotoRepository
import com.intelitec.fotostecnica.ui.ZoomableImage
import com.intelitec.fotostecnica.ui.theme.FotosTecnicaTheme
import kotlin.math.absoluteValue

class RevistaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FotosTecnicaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF11161C)
                ) {
                    RevistaScreen(onVolver = { finish() })
                }
            }
        }
    }
}

@Composable
fun RevistaScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val fotos = remember {
        FotoRepository.cargarFotos(contexto).sortedBy { it.orden }
    }

    if (fotos.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No hay fotos aún",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = "Añade fotos desde el menú principal para crear tu revista.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
            )
            TextButton(
                onClick = onVolver,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Volver", color = Color(0xFF9FD0B0))
            }
        }
        return
    }

    RevistaPager(fotos = fotos, onVolver = onVolver)
}

@Composable
private fun RevistaPager(fotos: List<Foto>, onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val estadoPager = rememberPagerState(initialPage = 0) { fotos.size }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = estadoPager,
            key = { fotos[it].id },
            contentPadding = PaddingValues(0.dp),
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { pagina ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Efecto "hojear revista": la pagina que entra/sale se inclina un poco
                        val offsetPagina =
                            (estadoPager.currentPage - pagina) + estadoPager.currentPageOffsetFraction
                        val abs = offsetPagina.absoluteValue.coerceIn(0f, 1f)
                        alpha = (1f - abs * 0.35f).coerceAtLeast(0.55f)
                        rotationY = -offsetPagina * 18f
                        val escala = (1f - abs * 0.06f).coerceAtLeast(0.86f)
                        scaleX = escala
                        scaleY = escala
                        cameraDistance = 30f * density
                    }
            ) {
                ZoomableImage(
                    modelo = FotoRepository.ruta(contexto, fotos[pagina]),
                    contentDescription = fotos[pagina].titulo.ifBlank { fotos[pagina].nombre }
                )

                // Titulo/descripcion como nota al pie estilo revista
                if (fotos[pagina].titulo.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp, start = 24.dp, end = 24.dp)
                    ) {
                        Text(
                            text = fotos[pagina].titulo,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                        if (fotos[pagina].descripcion.isNotBlank()) {
                            Text(
                                text = fotos[pagina].descripcion,
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Boton para volver
        IconButton(
            onClick = onVolver,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White
            )
        }

        // Indicador de pagina (ej. "3 / 15")
        Text(
            text = "${estadoPager.currentPage + 1} / ${fotos.size}",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}