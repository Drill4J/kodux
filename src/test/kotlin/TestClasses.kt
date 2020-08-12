package com.epam.kodux

import kotlinx.serialization.*


enum class EN {
    B, C
}

@Serializable
data class CompositeId(
    val str: String,
    val num: Int
) : Comparable<CompositeId> {
    override fun compareTo(other: CompositeId): Int = run {
        str.compareTo(other.str).takeIf { it != 0 } ?: num.compareTo(other.num)
    }
}

@Serializable
data class StoreMe(
    @Id val id: String
)

@Serializable
data class CompositeData(
    @Id val id: CompositeId,
    val data: String
)

@Serializable
data class MapField(
    @Id val id: String,
    val map: Map<EN, TempObject> = emptyMap()
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
    @Id val id: String,
    val set: MutableSet<SetPayload>
)

@Serializable
data class ObjectWithByteArray(
    @Id val id: String,
    val array: ByteArray
) {
    override fun equals(other: Any?): Boolean = this === other || other is ObjectWithByteArray && id == other.id

    override fun hashCode(): Int = id.hashCode()
}

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

@Serializable
data class ObjectWithList(@Id val id: String, val primitiveList: List<Boolean>)

@Serializable
data class ObjectWithSet(@Id val id: String, val primitiveSet: Set<Boolean>)

@Serializable
data class ObjectWithListOfComplexObject(@Id val id: String, val list: List<TempObject>)
