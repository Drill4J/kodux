package com.epam.xodus

import kotlinx.serialization.*
import kotlin.reflect.full.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Id


inline fun<reified T:Any> idPair(any:T):Pair<String?, Any?> {
    val idName = idName(T::class.serializer().descriptor)
    return  idName to  (T::class.memberProperties.find { it.name == idName })?.getter?.invoke(any)
}

fun idName(desc: SerialDescriptor) =
    (0 until desc.elementsCount).filter { inx -> desc.getElementAnnotations(inx).any { it is Id } }
        .map { idIndex -> desc.getElementName(idIndex) }.firstOrNull()
