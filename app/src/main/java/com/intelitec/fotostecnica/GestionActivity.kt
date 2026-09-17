package com.intelitec.fotostecnica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.intelitec.fotostecnica.data.Foto
import com.intelitec.fotostecnica.data.FotoRepository
import com.intelitec.fotostecnica.ui.theme.FotosTecnicaTheme
import java.io.File
import kotlin.math.roundToInt

class GestionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FotosTecnicaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GestionScreen(onVolver = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current

    var fotos by remember {
        mutableStateOf(
            FotoRepository.cargarFotos(contexto).sortedBy { it.orden }
        )
    }
    var seleccionadas by remember { mutableStateOf(setOf<String>()) }
    var fotoParaEditar by remember { mutableStateOf<Foto?>(null) }
    var mostrarConfirmacionBorrado by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestionar fotos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { mostrarConfirmacionBorrado = true },
                        enabled = seleccionadas.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar seleccionadas",
                            tint = if (seleccionadas.isNotEmpty())
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            )
        }
    ) { paddingInterno ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingInterno)
        ) {
            if (fotos.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No hay fotos aún",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "Añade fotos desde el menú principal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                ReordenableGrid(
                    fotos = fotos,
                    seleccionadas = seleccionadas,
                    onAlternarSeleccion = { id ->
                        seleccionadas = if (id in seleccionadas) seleccionadas - id
                                        else seleccionadas + id
                    },
                    onEditar = { fotoParaEditar = it },
                    onMover = { desde, hasta ->
                        val reordenadas = fotos.toMutableList()
                        val elemento = reordenadas.removeAt(desde)
                        reordenadas.add(hasta, elemento)
                        fotos = reordenadas
                        FotoRepository.guardarOrden(contexto, reordenadas)
                    }
                )
            }
        }
    }

    // Dialogo para editar titulo/descripcion
    fotoParaEditar?.let { foto ->
        DialogoEditarFoto(
            foto = foto,
            onGuardar = { editada ->
                val actualizadas = fotos.map { if (it.id == editada.id) editada else it }
                fotos = actualizadas
                FotoRepository.actualizarFoto(contexto, editada)
                fotoParaEditar = null
            },
            onCancelar = { fotoParaEditar = null }
        )
    }

    // Confirmacion antes de borrar
    if (mostrarConfirmacionBorrado) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrado = false },
            title = { Text("Eliminar fotos") },
            text = {
                Text(
                    "¿Eliminar ${seleccionadas.size} foto" +
                        (if (seleccionadas.size == 1) "" else "s") +
                        "? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val aBorrar = fotos.filter { it.id in seleccionadas }
                        FotoRepository.eliminarFotos(contexto, aBorrar)
                        fotos = fotos.filterNot { it.id in seleccionadas }
                        seleccionadas = emptySet()
                        mostrarConfirmacionBorrado = false
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionBorrado = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ReordenableGrid(
    fotos: List<Foto>,
    seleccionadas: Set<String>,
    onAlternarSeleccion: (String) -> Unit,
    onEditar: (Foto) -> Unit,
    onMover: (Int, Int) -> Unit
) {
    val contexto = LocalContext.current
    val columnas = 3
    val separacion = 8.dp
    val densidad = contexto.resources.displayMetrics.density

    var anchoGridPx by remember { mutableIntStateOf(0) }
    var idArrastrado by remember { mutableStateOf<String?>(null) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    // Valores "frescos" para leer dentro de los callbacks del gesto (evitan closures obsoletos)
    val fotosActualizadas by rememberUpdatedState(fotos)
    val anchoActualizado by rememberUpdatedState(anchoGridPx)
    val onMoverActualizado by rememberUpdatedState(onMover)
    val onAlternarSeleccionActualizado by rememberUpdatedState(onAlternarSeleccion)
    val onEditarActualizado by rememberUpdatedState(onEditar)

    // Tamano de cada celda (celdas cuadradas con separacion uniforme)
    val espacioPx = (8.dp * densidad)

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnas),
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { if (anchoGridPx == 0) anchoGridPx = it.width },
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(separacion),
        verticalArrangement = Arrangement.spacedBy(separacion)
    ) {
        items(fotos, key = { it.id }) { foto ->
            val esArrastrada = idArrastrado == foto.id
            val seleccionada = foto.id in seleccionadas

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (esArrastrada) Modifier.zIndex(1f) else Modifier.zIndex(0f)
                    )
                    .offset {
                        if (esArrastrada) {
                            IntOffset(
                                desplazamiento.x.roundToInt(),
                                desplazamiento.y.roundToInt()
                            )
                        } else {
                            IntOffset.Zero
                        }
                    }
                    .clickable { onAlternarSeleccionActualizado(foto.id) }
                    .pointerInput(foto.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                idArrastrado = foto.id
                                desplazamiento = Offset.Zero
                            },
                            onDrag = { cambio, cantidad ->
                                cambio.consume()
                                desplazamiento += cantidad

                                // Dentro del callback se leen los valores actualizados
                                val yCeldas = (anchoActualizado - 2 * espacioPx) / columnas.toFloat()
                                if (yCeldas <= 0f) return@detectDragGesturesAfterLongPress
                                val pasoY = yCeldas + espacioPx

                                val indiceActual = fotosActualizadas.indexOfFirst { it.id == foto.id }
                                if (indiceActual < 0) return@detectDragGesturesAfterLongPress

                                // Columna/fila objetivo a partir del desplazamiento acumulado
                                val deltaCol = (desplazamiento.x / pasoY).roundToInt()
                                val deltaFila = (desplazamiento.y / pasoY).roundToInt()

                                val colActual = indiceActual % columnas
                                val filaActual = indiceActual / columnas
                                val colObj = (colActual + deltaCol).coerceIn(0, columnas - 1)
                                val filaObj =
                                    (filaActual + deltaFila).coerceIn(0, fotosActualizadas.lastIndex / columnas)
                                val objetivo =
                                    (filaObj * columnas + colObj).coerceIn(0, fotosActualizadas.lastIndex)

                                if (objetivo != indiceActual) {
                                    // Compensar para que la foto no "brinque" al reordenarse
                                    onMoverActualizado(indiceActual, objetivo)
                                    desplazamiento -= Offset(
                                        (colObj - colActual) * pasoY,
                                        (filaObj - filaActual) * pasoY
                                    )
                                }
                            },
                            onDragEnd = {
                                idArrastrado = null
                                desplazamiento = Offset.Zero
                            },
                            onDragCancel = {
                                idArrastrado = null
                                desplazamiento = Offset.Zero
                            }
                        )
                    }
            ) {
                AsyncImage(
                    model = File(FotoRepository.ruta(contexto, foto).path),
                    contentDescription = foto.titulo.ifBlank { foto.nombre },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Checkbox de seleccion
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (seleccionada) MaterialTheme.colorScheme.primary
                            else Color.Black.copy(alpha = 0.35f)
                        )
                        .border(
                            width = 2.dp,
                            color = if (seleccionada) Color.White else Color.White.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (seleccionada) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seleccionada",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Boton de editar titulo/descripcion
                IconButton(
                    onClick = { onEditarActualizado(foto) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(34.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar título o descripción",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Indicador de modo arrastre
                if (esArrastrada) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Arrastrando",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoEditarFoto(
    foto: Foto,
    onGuardar: (Foto) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo by remember(foto.id) { mutableStateOf(foto.titulo) }
    var descripcion by remember(foto.id) { mutableStateOf(foto.descripcion) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Editar foto") },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onGuardar(
                        foto.copy(
                            titulo = titulo.trim(),
                            descripcion = descripcion.trim()
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}