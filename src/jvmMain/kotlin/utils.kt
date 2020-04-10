package com.epam.kodux

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.reflect.full.*

const val SIZE_PROPERTY_NAME = "size"

val json = Json(JsonConfiguration.Stable)

inline fun <reified T : Any> idPair(any: T): Pair<String?, Any?> {
    val idName = idName(T::class.serializer().descriptor)
    return idName to (T::class.memberProperties.find { it.name == idName })?.getter?.invoke(any)
}

fun idName(desc: SerialDescriptor): String? = (0 until desc.elementsCount).firstOrNull { inx ->
    desc.getElementAnnotations(inx).any { it is Id }
}?.let { idIndex -> desc.getElementName(idIndex) }

fun Any.encodeId(): String = this as? String ?: json.stringify(unchecked(this::class.serializer()), this)

fun <T> String.decodeId(deser: DeserializationStrategy<T>): T = json.parse(deser, this)

@Suppress("UNCHECKED_CAST")
fun <T> unchecked(any: Any) = any as T
