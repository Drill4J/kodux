package com.epam.kodux

import kotlinx.serialization.*


enum class EnumExample {
    FIRST,
    SECOND
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
    val map: Map<EnumExample, TempObject> = emptyMap()
)

@Serializable
data class ComplexObject(
    @Id val id: String,
    val ch: Char?,
    val blink: SubObject?,
    val enumExample: EnumExample = EnumExample.FIRST,
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
data class ObjectWithDefaults(
    @Id val id: String,
    val payload: AllDefaultPayload = AllDefaultPayload()
)

@Serializable
data class AllDefaultPayload(
    val num: Int = 0,
    val str: String = "",
    val list: List<String> = emptyList()
)

@Serializable
data class SetPayload(
    val id: String,
    val name: String
)

@Serializable
data class SubObject(
    val string: String,
    val int: Int,
    val last: Last
)

@Serializable
data class SimpleObject(
    @Id val id: String,
    val string: String,
    val int: Int,
    val last: Last
)

@Serializable
data class Last(val string: Byte)


@Serializable
data class TempObject(
    val st: String,
    val int: Int
)

@Serializable
data class ObjectWithPrimitiveElementsCollection(
    @Id val id: Int,
    val st: List<String>
)

@Serializable
data class ObjectWithReferenceElementsCollection(
    @Id val id: Int,
    val st: Set<TempObject>
)

@Serializable
data class ObjectWithPrimitiveElementsMap(
    @Id val id: Int,
    val st: Map<String, Int>
)

@Serializable
data class ObjectWithReferenceElementsMap(
    @Id val id: Int,
    val st: Map<TempObject, TempObject>
)

@Serializable
data class ObjectWithReferenceElementsMapMixed(
    @Id val id: Int,
    val st: Map<String, TempObject>
)
