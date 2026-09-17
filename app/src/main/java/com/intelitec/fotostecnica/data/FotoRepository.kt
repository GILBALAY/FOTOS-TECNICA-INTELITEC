package com.intelitec.fotostecnica.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import java.io.File
import java.util.UUID

/**
 * Repositorio de fotos. Trabaja 100% offline usando el almacenamiento interno de la app:
 *
 *   /data/data/com.intelitec.fotostecnica/files/fotos/  -> imagenes copiadas
 *   /data/data/com.intelitec.fotostecnica/files/fotos.json -> metadatos (JSON)
 *
 * Al copiar una foto de la galeria se crea una copia persistente en [carpetaFotos],
 * de modo que la URI temporal de la galeria no es necesaria despues de la importacion.
 */
object FotoRepository {

    private const val ARCHIVO_METADATOS = "fotos.json"

    fun carpetaFotos(context: Context): File =
        File(context.filesDir, "fotos").apply { mkdirs() }

    private fun archivoMetadatos(context: Context): File =
        File(context.filesDir, ARCHIVO_METADATOS)

    /** Ruta fisica del archivo de imagen dentro del almacenamiento interno. */
    fun ruta(context: Context, foto: Foto): File =
        File(carpetaFotos(context), foto.archivo)

    /** Carga los metadatos y los devuelve ordenados por [Foto.orden]. */
    fun cargarFotos(context: Context): MutableList<Foto> {
        val archivo = archivoMetadatos(context)
        if (!archivo.exists()) return mutableListOf()
        return try {
            val arrInverso = JSONArray(archivo.readText())
            val lista = (0 until arrInverso.length())
                .map { Foto.desdeJson(arrInverso.getJSONObject(it)) }
                .sortedBy { it.orden }
                .toMutableList()
            // Reasigna orden consecutivo para evitar huecos
            lista.indices.forEach { lista[it] = lista[it].copy(orden = it) }
            lista
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    /** Guarda la lista completa de metadatos en el JSON. */
    fun guardarFotos(context: Context, fotos: List<Foto>) {
        try {
            val arr = JSONArray()
            fotos.sortedBy { it.orden }.forEach { arr.put(it.toJson()) }
            archivoMetadatos(context).writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    /**
     * Copia cada URI de la galeria al almacenamiento interno y registra los metadatos.
     * Devuelve cuantas fotos se importaron correctamente.
     */
    fun copiarFotos(context: Context, uris: List<Uri>): Int {
        if (uris.isEmpty()) return 0
        val dir = carpetaFotos(context)
        val actuales = cargarFotos(context).toMutableList()
        var siguienteOrden = (actuales.maxOfOrNull { it.orden } ?: -1) + 1
        var importadas = 0

        for (uri in uris) {
            try {
                val original = nombreDeUri(context, uri) ?: "foto_${System.currentTimeMillis()}.jpg"
                val extension = original.substringAfterLast('.', "").ifBlank { "jpg" }
                val id = UUID.randomUUID().toString()
                val archivo = "$id.$extension"
                val destino = File(dir, archivo)

                val entrada = context.contentResolver.openInputStream(uri) ?: continue
                entrada.use { input ->
                    destino.outputStream().use { output -> input.copyTo(output) }
                }

                actuales.add(Foto(id = id, nombre = original, archivo = archivo, orden = siguienteOrden++))
                importadas++
            } catch (_: Exception) {
                // Se ignora el fallo de una foto concreta para que el resto siga importandose
            }
        }

        if (importadas > 0) guardarFotos(context, actuales)
        return importadas
    }

    /** Elimina las fotos indicadas (archivo fisico + metadatos). */
    fun eliminarFotos(context: Context, fotos: List<Foto>) {
        if (fotos.isEmpty()) return
        val ids = fotos.map { it.id }.toSet()
        val restantes = cargarFotos(context).filterNot { it.id in ids }
        fotos.forEach { ruta(context, it).delete() }
        guardarFotos(context, restantes)
    }

    /** Reemplaza titulo/descripcion de una foto existente. */
    fun actualizarFoto(context: Context, fotoEditada: Foto) {
        val actuales = cargarFotos(context)
        guardarFotos(
            context,
            actuales.map { if (it.id == fotoEditada.id) fotoEditada.copy(orden = it.orden) else it }
        )
    }

    /** Asigna el orden consecutivo segun la posicion actual en la lista. */
    fun guardarOrden(context: Context, fotos: List<Foto>) {
        val reordenadas = fotos.mapIndexed { i, f -> f.copy(orden = i) }
        guardarFotos(context, reordenadas)
    }

    private fun nombreDeUri(context: Context, uri: Uri): String? {
        var nombre: String? = null
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) nombre = cursor.getString(idx)
            }
        }
        return nombre
    }
}