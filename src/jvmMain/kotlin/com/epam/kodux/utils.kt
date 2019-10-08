package com.epam.kodux

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.serializer
import kotlin.reflect.full.memberProperties

const val SIZE_PROPERTY_NAME = "size"

inline fun <reified T : Any> idPair(any: T): Pair<String?, Any?> {
    val idName = idName(T::class.serializer().descriptor)
    return idName to (T::class.memberProperties.find { it.name == idName })?.getter?.invoke(any)
}

fun idName(desc: SerialDescriptor) =
        (0 until desc.elementsCount).filter { inx -> desc.getElementAnnotations(inx).any { it is Id } }
                .map { idIndex -> desc.getElementName(idIndex) }.firstOrNull()
