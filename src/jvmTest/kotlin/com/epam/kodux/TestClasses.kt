package com.epam.kodux

import kotlinx.serialization.Serializable


enum class EN {
    B, C
}

@Serializable
data class MapField(
    @Id
    val id: String,
    val map: Map<EN, TempObject>
)

@Serializable
data class ComplexObject(
    @Id val id: String,
    val ch: Char?,
    val blink: SubObject?,
    val en: EN = EN.B,
    val nullString: String?
)

@Serializable
data class ObjectWithSetField(
    @Id
    val id: String,
    val set: MutableSet<SetPayload>
)

@Serializable
data class ObjectWithByteArray(
    @Id
    val id: String,
    val array: ByteArray
)

@Serializable
data class SetPayload(val id: String, val name: String)

@Serializable
data class SubObject(val sub_string: String, val sub_int: Int, val sub_last: Last)


@Serializable
data class Last(val string: Byte)


@Serializable
data class TempObject(val st: String, val int: Int)

@Serializable
data class ObjectWithPrimitiveElementsCollection(val st: List<String>, @Id val id: Int)

@Serializable
data class ObjectWithReferenceElementsCollection(val st: Set<TempObject>, @Id val id: Int)

@Serializable
data class ObjectWithPrimitiveElementsMap(val st: Map<String, Int>, @Id val id: Int)

@Serializable
data class ObjectWithReferenceElementsMap(val st: Map<TempObject, TempObject>, @Id val id: Int)

@Serializable
data class ObjectWithReferenceElementsMapMixed(val st: Map<String, TempObject>, @Id val id: Int)
