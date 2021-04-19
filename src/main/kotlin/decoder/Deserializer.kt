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
package com.epam.kodux.decoder

import com.epam.kodux.*
import com.epam.kodux.util.*
import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import mu.*
import org.apache.commons.compress.compressors.zstandard.*
import org.nustaq.serialization.*
import java.io.*

private val logger = KotlinLogging.logger { }

class XodusDecoder(
    private val txn: StoreTransaction,
    private val classLoader: ClassLoader,
    private val ent: Entity,
    private val idName: String? = null,
) : Decoder, CompositeDecoder {
    override val serializersModule: SerializersModule = EmptySerializersModule

    private fun SerialDescriptor.getTag(index: Int): String {
        return this.getElementName(index)
    }

    private fun decodeTaggedBoolean(tag: String): Boolean {
        return ent.getProperty(tag) as Boolean
    }

    private fun decodeTaggedByte(tag: String): Byte {
        return ent.getProperty(tag) as Byte
    }

    private fun decodeTaggedChar(tag: String): Char {
        return ent.getProperty(tag).toString()[0]
    }

    private fun decodeTaggedDouble(tag: String): Double {
        return ent.getProperty(tag) as Double
    }

    private fun decodeTaggedEnum(tag: String): Int {
        return ent.getProperty(tag) as Int
    }

    private fun decodeTaggedFloat(tag: String): Float {
        return ent.getProperty(tag) as Float
    }

    private fun decodeTaggedInt(tag: String): Int {
        return ent.getProperty(tag) as Int
    }

    private fun decodeTaggedLong(tag: String): Long {
        return ent.getProperty(tag) as Long
    }

    private fun decodeTaggedNotNullMark(@Suppress("UNUSED_PARAMETER") tag: String): Boolean {
        return ent.getProperty(tag) != null || ent.getLink(tag) != null
    }

    private fun decodeTaggedShort(tag: String): Short {
        return ent.getProperty(tag) as Short
    }

    private fun decodeTaggedString(tag: String): String {
        return ent.getProperty(tag) as String
    }

    private fun <T> decodeTaggedObject(
        tag: String,
        des: DeserializationStrategy<T>,
        deserializationSettings: SerializationSettings?,
    ): T {
        return restoreObject(des, ent, tag, deserializationSettings)
    }

    private fun <T> restoreObject(
        des: DeserializationStrategy<T>,
        ent: Entity,
        tag: String,
        deserializationSettings: SerializationSettings? = null,
    ): T = if (deserializationSettings != null) {
        streamDeserialization(deserializationSettings, ent, tag)
    } else {
        kotlinxDeserialization(des, ent, tag)
    }


    private fun <T> streamDeserialization(
        deserializationSettings: SerializationSettings,
        ent: Entity,
        tag: String,
    ) = when (deserializationSettings.serializationType) {
        SerializationType.FST -> {
            val fst: FSTConfiguration = FSTConfiguration.getDefaultConfiguration().also {
                it.streamCoderFactory = StreamDecoderFactory(it)
                it.classLoader = classLoader
            }
            val blob = ent.getProperty(tag) as String
            when (deserializationSettings.compressType) {
                CompressType.ZSTD -> ZstdCompressorInputStream(File(blob).inputStream())
                else -> File(blob).inputStream()
            }.use {
                logger.trace { "Reading entity: ${ent.type} from file: $blob" }
                @Suppress("UNCHECKED_CAST")
                fst.decodeFromStream(it) as T
            }
        }
    }

    private fun <T> kotlinxDeserialization(
        des: DeserializationStrategy<T>,
        ent: Entity,
        tag: String,
    ) = when (des) {
        ByteArraySerializer() -> {
            @Suppress("UNCHECKED_CAST")
            ent.getBlob(tag)?.readBytes() as T
        }
        is MapLikeSerializer<*, *, *, *> -> {
            val link = checkNotNull(ent.getLink(tag)) { "$tag nullable collections are not supported yet" }
            val size = link.getProperty(SIZE_PROPERTY_NAME) as Int
            val associateWith = (0 until size).associate {
                val k = parseElement(des.keySerializer, link, "k$it")
                val v = parseElement(des.valueSerializer, link, "v$it")
                k to v
            }
            unchecked(associateWith)
        }
        is AbstractCollectionSerializer<*, *, *> -> {
            val elementDescriptor = des.descriptor.getElementDescriptor(0)
            val serialKind = elementDescriptor.kind
            val objects: Iterable<Any> = if (serialKind !is PrimitiveKind) {
                val deserializer = classLoader.loadClass(elementDescriptor.serialName).kotlin.serializer()
                ent.getLinks(tag).mapTo(des.descriptor.outputCollection()) { entity ->
                    XodusDecoder(txn, classLoader, entity).decodeSerializableValue(deserializer)
                }
            } else {
                val link = ent.getLink(tag)!!
                val size = link.getProperty(SIZE_PROPERTY_NAME) as Int
                (0 until size).mapTo(des.descriptor.outputCollection()) {
                    link.getProperty(it.toString()) as Comparable<*>
                }
            }
            val list = parseCollection(des, objects)
            unchecked(list)
        }
        else ->
            when {
                tag == idName -> decodeTaggedString(tag).decodeId(des)
                des.descriptor.kind == SerialKind.ENUM -> decodeSerializableValue(des)
                else -> XodusDecoder(
                    txn = txn,
                    classLoader = classLoader,
                    ent = checkNotNull(ent.getLink(tag)) { "should be not null $tag" }
                ).decodeSerializableValue(des)
            }

    }

    private fun parseElement(targetSerializer: KSerializer<out Any?>, link: Entity, propertyName: String): Any? {
        if (targetSerializer.descriptor.kind == SerialKind.ENUM) {
            return classLoader.loadClass(targetSerializer.descriptor.serialName).enumConstants[link.getProperty(
                propertyName
            ) as Int]
        }
        return when (targetSerializer) {
            !is GeneratedSerializer<*> -> {
                link.getProperty(propertyName) ?: restoreObject(targetSerializer, link, propertyName)
            }
            else -> XodusDecoder(txn, classLoader, link.getLink(propertyName)!!).decodeSerializableValue(
                targetSerializer
            )
        }
    }

    private fun parseCollection(
        des: AbstractCollectionSerializer<*, *, *>,
        objects: Iterable<Any>,
    ): Any = run {
        objects.firstOrNull()?.let { it::class.serializer() }?.let { serializer ->
            when (des::class) {
                ListSerializer(serializer)::class,
                SetSerializer(serializer)::class,
                -> objects
                else -> TODO("not implemented yet")
            }
        } ?: objects
    }

    override fun decodeNotNullMark(): Boolean = decodeTaggedNotNullMark(currentTag)
    override fun decodeNull(): Nothing? = null
    override fun decodeBoolean(): Boolean = decodeTaggedBoolean(popTag())
    override fun decodeByte(): Byte = decodeTaggedByte(popTag())
    override fun decodeShort(): Short = decodeTaggedShort(popTag())
    override fun decodeInt(): Int = decodeTaggedInt(popTag())
    override fun decodeLong(): Long = decodeTaggedLong(popTag())
    override fun decodeFloat(): Float = decodeTaggedFloat(popTag())
    override fun decodeDouble(): Double = decodeTaggedDouble(popTag())
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = decodeTaggedEnum(popTag())

    override fun decodeChar(): Char = decodeTaggedChar(popTag())
    override fun decodeString(): String = decodeTaggedString(popTag())

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
    }

    override fun decodeSequentially(): Boolean = true

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
        decodeTaggedBoolean(descriptor.getTag(index))

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
        decodeTaggedByte(descriptor.getTag(index))

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
        decodeTaggedShort(descriptor.getTag(index))

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
        decodeTaggedInt(descriptor.getTag(index))

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
        decodeTaggedLong(descriptor.getTag(index))

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
        decodeTaggedFloat(descriptor.getTag(index))

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
        decodeTaggedDouble(descriptor.getTag(index))

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
        decodeTaggedChar(descriptor.getTag(index))

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
        decodeTaggedString(descriptor.getTag(index))

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T =
        tagBlock(descriptor.getTag(index)) {
            decodeTaggedObject(
                descriptor.getTag(index),
                deserializer,
                getSerializationSettings(descriptor.getElementAnnotations(index))
            )
        }

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
    ): T? =
        tagBlock(descriptor.getTag(index)) {
            if (deserializer is GeneratedSerializer)
                decodeTaggedObject(
                    descriptor.getTag(index),
                    deserializer,
                    getSerializationSettings(descriptor.getElementAnnotations(index))
                )
            else
                decodeNullableSerializableValue(deserializer)

        }

    private fun <E> tagBlock(tag: String, block: () -> E): E {
        pushTag(tag)
        val r = block()
        if (!flag) {
            popTag()
        }
        flag = false
        return r
    }

    private val tagStack = arrayListOf<String>()
    private val currentTag: String
        get() = tagStack.last()

    private fun pushTag(name: String) {
        tagStack.add(name)
    }

    private var flag = false

    private fun popTag(): String {
        val r = tagStack.removeAt(tagStack.lastIndex)
        flag = true
        return r
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = TODO("not implemented yet")

    override fun endStructure(descriptor: SerialDescriptor) = Unit
}

private fun <T> SerialDescriptor.outputCollection(): MutableCollection<T> = kotlin.run {
    if (HashSet::class.simpleName!! in serialName) mutableSetOf() else mutableListOf()
}
