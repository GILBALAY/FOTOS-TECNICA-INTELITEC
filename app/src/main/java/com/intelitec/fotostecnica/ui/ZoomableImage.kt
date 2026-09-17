package com.intelitec.fotostecnica.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import coil.compose.AsyncImage

private const val ESCALA_MAXIMA = 5f
private const val ESCALA_DOBLE_TAP = 2.5f

/** Estado compartido de zoom/pan de una imagen. */
class ZoomState {
    var escala by mutableFloatStateOf(1f)
    var desplazamiento by mutableStateOf(Offset.Zero)

    /** Tamano del contenedor, necesario para acotar el desplazamiento. */
    var tamanoContenedor by mutableStateOf(Size.Unspecified)

    fun reiniciar() {
        escala = 1f
        desplazamiento = Offset.Zero
    }

    /** Limita el desplazamiento para que la imagen nunca se salga por completo del area visible. */
    fun acotar() {
        if (!tamanoContenedor.isSpecified || tamanoContenedor.width <= 0f) return
        val maxX = (escala - 1f) * tamanoContenedor.width / 2f
        val maxY = (escala - 1f) * tamanoContenedor.height / 2f
        val acotadoX = desplazamiento.x.coerceIn(-maxX, maxX)
        val acotadoY = desplazamiento.y.coerceIn(-maxY, maxY)
        desplazamiento = Offset(acotadoX, acotadoY)
    }
}

/**
 * Imagen que se puede ampliar con pellizco (dos dedos) y doble toque.
 *
 * - Un dedo sin zoom: deja pasar el gesto al HorizontalPager para hojear.
 * - Dos dedos: hace zoom/pan (gesto consumido, el pager no se mueve).
 * - Con zoom activo, un dedo permite desplazar la imagen.
 */
@Composable
fun ZoomableImage(
    modelo: Any?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val estado = remember { ZoomState() }

    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
            .onSizeChanged {
                estado.tamanoContenedor = Size(it.width.toFloat(), it.height.toFloat())
            }
            .gestoZoom(estado)
            .pointerInput(estado) {
                detectTapGestures(
                    onDoubleTap = { punto ->
                        if (estado.escala > 1.01f) {
                            estado.reiniciar()
                        } else {
                            val centro = Offset(
                                estado.tamanoContenedor.width / 2f,
                                estado.tamanoContenedor.height / 2f
                            )
                            estado.escala = ESCALA_DOBLE_TAP
                            estado.desplazamiento = (punto - centro) * (ESCALA_DOBLE_TAP - 1f) * -1f
                            estado.acotar()
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = modelo,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = estado.escala
                    scaleY = estado.escala
                    translationX = estado.desplazamiento.x
                    translationY = estado.desplazamiento.y
                    transformOrigin = TransformOrigin.Center
                }
        )
    }
}

/**
 * Modificador de gestos de zoom/pan.
 *
 * La regla clave: mientras hay UN solo dedo y la imagen no esta ampliada,
 * no se consume ningun evento, de modo que el HorizontalPager padre puede
 * interpretar el deslizamiento horizontal para pasar de pagina.
 */
private fun Modifier.gestoZoom(estado: ZoomState): Modifier = pointerInput(estado) {

    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)

        var anclaPan: Offset? = null
        var origenPan = Offset.Zero
        var huboPellizco = false

        while (true) {
            val evento = awaitPointerEvent()
            val pulsados = evento.changes.filter { it.pressed }

            if (pulsados.isEmpty()) break

            if (pulsados.size >= 2) {
                // Pellizco con dos dedos: aplicar escala y desplazamiento
                huboPellizco = true
                anclaPan = null
                val zoom = evento.calculateZoom()
                val pan = evento.calculatePan()
                estado.escala = (estado.escala * zoom).coerceIn(1f, ESCALA_MAXIMA)
                estado.desplazamiento += pan
                estado.acotar()
                evento.changes.forEach { if (it.positionChanged()) it.consume() }
            } else {
                val dedo = pulsados.first()
                val pellizcoPrevio = huboPellizco
                huboPellizco = false

                if (estado.escala > 1.01f) {
                    // Un dedo con zoom activo: desplazar la imagen
                    if (anclaPan == null) {
                        anclaPan = dedo.position
                        origenPan = estado.desplazamiento
                    }
                    estado.desplazamiento = origenPan + (dedo.position - anclaPan!!)
                    estado.acotar()
                    if (dedo.positionChanged()) dedo.consume()
                } else if (pellizcoPrevio) {
                    // Al terminar un pellizco sin zoom, permitir que el pager retome el gesto
                    anclaPan = null
                }
            }
        }
    }
}