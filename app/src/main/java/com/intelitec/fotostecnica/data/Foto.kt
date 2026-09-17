package com.intelitec.fotostecnica.data

import org.json.JSONObject
import java.util.UUID

/**
 * Metadatos de cada foto guardada en el almacenamiento interno.
 *
 * @param id          Identificador unico.
 * @param nombre      Nombre de archivo original (como llego de la galeria).
 * @param archivo     Nombre del archivo copiado al almacenamiento interno.
 * @param titulo      Titulo/leyenda introducido por el usuario (opcional).
 * @param descripcion Descripcion introducida por el usuario (opcional).
 * @param orden       Posicion dentro de la revista (0..n-1).
 */
data class Foto(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val archivo: String,
    val titulo: String = "",
    val descripcion: String = "",
    val orden: Int = 0
) {

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("nombre", nombre)
        .put("archivo", archivo)
        .put("titulo", titulo)
        .put("descripcion", descripcion)
        .put("orden", orden)

    companion object {
        fun desdeJson(o: JSONObject): Foto = Foto(
            id = o.optString("id", UUID.randomUUID().toString()),
            nombre = o.optString("nombre", "foto"),
            archivo = o.optString("archivo"),
            titulo = o.optString("titulo"),
            descripcion = o.optString("descripcion"),
            orden = o.optInt("orden", 0)
        )
    }
}