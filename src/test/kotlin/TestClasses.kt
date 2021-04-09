/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.kodux

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class TestAnnotation

enum class EnumExample {
    FIRST,
    SECOND
}

@Serializable
data class CompositeId(
    val str: String,
    val num: Int,
) : Comparable<CompositeId> {
    override fun compareTo(other: CompositeId): Int = run {
        str.compareTo(other.str).takeIf { it != 0 } ?: num.compareTo(other.num)
    }
}

@Serializable
data class StoreMe(
    @Id val id: String,
)

@Serializable
data class CompositeData(
    @Id val id: CompositeId,
    val data: String,
)

@Serializable
data class StreamSerializationTestObject(
    @Id val id: CompositeId,
    @StreamSerialization(SerializationType.FST, CompressType.ZSTD)
    val list: List<String>,
) : java.io.Serializable

@Serializable
data class MapInMapWrapper(
    @Id val id: CompositeId,
    val map: MutableMap<String, MapWrapper>,
) : java.io.Serializable

@Serializable
data class MapWrapper(
    val id: Int,
    @StreamSerialization(SerializationType.FST, CompressType.ZSTD)
    val secondMap: MutableMap<String, TestClass>,
) : java.io.Serializable

@Serializable
data class TestClass(val string: String, val int: Int) : java.io.Serializable


@Serializable
data class StreamSerializationKryoTestObject(
    @Id val id: CompositeId,
    @StreamSerialization(SerializationType.KRYO, CompressType.ZSTD)
    val map: List<String>,
)

@Serializable
data class StreamSerializationKryoTestObject1(
    @Id val id: CompositeId,
    @StreamSerialization(SerializationType.KRYO, CompressType.ZSTD)
    val map: TestClass,
)

@Serializable
data class StreamSerializationKryoTestObjectTest(
    @Id val id: CompositeId,
    @StreamSerialization(SerializationType.KRYO, CompressType.ZSTD)
    val testClass: TestClass,
)

@Serializable
data class MapInMapWrapperKryo(
    @Id val id: CompositeId,
    val map: MutableMap<String, MapWrapperKryo>,
)

@Serializable
data class MapWrapperKryo(
    val id: Int,
    @StreamSerialization(SerializationType.KRYO, CompressType.ZSTD)
    val secondMap: MutableMap<String, TestClass>,
)


@Serializable
data class MapField(
    @Id val id: String,
    val map: Map<EnumExample, TempObject> = emptyMap(),
)

@Serializable
data class ComplexObject(
    @Id val id: String,
    val ch: Char?,
    val blink: SubObject?,
    val enumExample: EnumExample = EnumExample.FIRST,
    val nullString: String?,
)

@Serializable
data class ComplexListNesting(
    @Id val id: String,
    val payload: PayloadWrapper = PayloadWrapper(),
)


@Serializable
data class PayloadWrapper(
    val type: String = "undefined",
    val payload: PayloadWithList = PayloadWithList(),
)

@Serializable
data class PayloadWithList(
    val num: Int = 0,
    val str: String = "",
    val list: List<SetPayload> = emptyList(),
)


@Serializable
data class ObjectWithSetField(
    @Id val id: String,
    val set: MutableSet<SetPayload>,
)

@Serializable
data class ObjectWithByteArray(
    @Id val id: String,
    val array: ByteArray,
) {
    override fun equals(other: Any?): Boolean = this === other || other is ObjectWithByteArray && id == other.id

    override fun hashCode(): Int = id.hashCode()
}

@Serializable
data class ObjectWithDefaults(
    @Id val id: String,
    val payload: AllDefaultPayload = AllDefaultPayload(),
)

@Serializable
data class AllDefaultPayload(
    val num: Int = 0,
    val str: String = "",
    val list: List<String> = emptyList(),
)

@Serializable
data class SetPayload(
    val id: String,
    val name: String,
)

@Serializable
data class SubObject(
    val string: String,
    val int: Int,
    val last: Last,
)

@Serializable
data class SimpleObject(
    @Id val id: String,
    val string: String,
    val int: Int,
    val last: Last,
)

@Serializable
data class Last(val string: Byte)


@Serializable
data class TempObject(
    val st: String,
    val int: Int,
)

@Serializable
data class ObjectWithPrimitiveElementsCollection(
    @Id val id: Int,
    val st: List<String>,
)

@Serializable
data class ObjectWithReferenceElementsCollection(
    @Id val id: Int,
    val st: Set<TempObject>,
)

@Serializable
data class ObjectWithPrimitiveElementsMap(
    @Id val id: Int,
    val st: Map<String, Int>,
)

@Serializable
data class ObjectWithReferenceElementsMap(
    @Id val id: Int,
    val st: Map<TempObject, TempObject>,
)

@Serializable
data class ObjectWithReferenceElementsMapMixed(
    @Id val id: Int,
    val st: Map<String, TempObject>,
)

@Serializable
data class ObjectWithTwoAnnotation(
    @TestAnnotation
    @Id val id: CompositeId,
    val size: Int,
)
