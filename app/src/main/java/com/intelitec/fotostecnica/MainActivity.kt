package com.intelitec.fotostecnica

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intelitec.fotostecnica.data.FotoRepository
import androidx.lifecycle.lifecycleScope
import com.intelitec.fotostecnica.ui.theme.FotosTecnicaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val estadoDatos = mutableIntStateOf(0)

    /** Selector de la galeria (Photo Picker del sistema, sin permisos en tiempo de ejecucion). */
    private val selectorFotos = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> -> importarFotos(uris) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FotosTecnicaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        versionDatos = estadoDatos.intValue,
                        onAñadirFotos = {
                            selectorFotos.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onVerRevista = { startActivity(Intent(this, RevistaActivity::class.java)) },
                        onGestionar = { startActivity(Intent(this, GestionActivity::class.java)) }
                    )
                }
            }
        }
    }

    /** Al volver de otra pantalla, se actualiza el contador de fotos en el menu. */
    override fun onResume() {
        super.onResume()
        estadoDatos.intValue = estadoDatos.intValue + 1
    }

    /** Copia las fotos seleccionadas al almacenamiento interno (fuera del hilo principal). */
    private fun importarFotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        lifecycleScope.launch(Dispatchers.IO) {
            val importadas = FotoRepository.copiarFotos(this@MainActivity, uris)
            withContext(Dispatchers.Main) {
                val mensaje = when {
                    importadas == 0 -> "No se pudo importar ninguna foto"
                    importadas == 1 -> "1 foto añadida"
                    else -> "$importadas fotos añadidas"
                }
                Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun MainScreen(
    versionDatos: Int,
    onAñadirFotos: () -> Unit,
    onVerRevista: () -> Unit,
    onGestionar: () -> Unit
) {
    val contexto = LocalContext.current
    val contador = remember(versionDatos) {
        FotoRepository.cargarFotos(contexto).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Mi Revista Digital",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "FOTOS TÉCNICA INTELITEC",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(56.dp))

        BotonMenu(
            texto = "Añadir fotos",
            icono = Icons.Default.AddPhotoAlternate,
            onClick = onAñadirFotos
        )
        Spacer(Modifier.height(16.dp))
        BotonMenu(
            texto = "Ver revista",
            icono = Icons.Default.MenuBook,
            onClick = onVerRevista
        )
        Spacer(Modifier.height(16.dp))
        BotonMenu(
            texto = "Gestionar fotos",
            icono = Icons.Default.ManageAccounts,
            onClick = onGestionar
        )

        Spacer(Modifier.height(48.dp))
        Text(
            text = if (contador == 0) "No hay fotos aún"
                   else "Tienes $contador foto${if (contador == 1) "" else "s"} guardadas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BotonMenu(
    texto: String,
    icono: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
    ) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(18.dp))
        Text(texto, style = MaterialTheme.typography.titleLarge)
    }
}