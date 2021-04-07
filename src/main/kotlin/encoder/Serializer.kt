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
package com.epam.kodux.encoder

import com.epam.kodux.*
import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import mu.*
import org.apache.commons.compress.compressors.zstandard.*
import org.nustaq.serialization.*
import java.io.*
import java.nio.file.*
import java.util.*

private val logger = KotlinLogging.logger { }

class XodusEncoder(
    private val txn: StoreTransaction,
    private val classLoader: ClassLoader,
    private val ent: Entity,
) : Encoder, CompositeEncoder {
    private fun SerialDescriptor.getTag(index: Int) = this.getElementName(index)

    override val serializersModule: SerializersModule
        get() = EmptySerializersModule

    private fun encodeTaggedBoolean(tag: String, value: Boolean) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedByte(tag: String, value: Byte) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedChar(tag: String, value: Char) {
        //doesn't support character
        ent.setProperty(tag, value.toString())
    }

    private fun encodeTaggedDouble(tag: String, value: Double) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedFloat(tag: String, value: Float) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedInt(tag: String, value: Int) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedLong(tag: String, value: Long) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedNull(@Suppress("UNUSED_PARAMETER") tag: String) {
    }

    private fun encodeTaggedShort(tag: String, value: Short) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedString(tag: String, value: String) {
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedObject(
        tag: String,
        value: Any,
        isId: Boolean,
        serializationSettings: SerializationSettings?,
        des: SerializationStrategy<Any>,
    ) {
        storeObject(des, isId, serializationSettings, value, ent, tag)
    }

    private fun storeObject(
        des: SerializationStrategy<*>,
        isId: Boolean,
        serializationSettings: SerializationSettings?,
        value: Any,
        ent: Entity,
        tag: String,
    ) {
        if (serializationSettings != null) {
            streamSerialization(serializationSettings, ent, value, tag)
        } else {
            kotlinxSerialization(value, ent, tag, des, isId)
        }
    }

    private fun kotlinxSerialization(
        value: Any,
        ent: Entity,
        tag: String,
        des: SerializationStrategy<*>,
        isId: Boolean,
    ) = when (value) {
        is ByteArray -> {
            ent.setBlob(tag, value.inputStream())
        }
        is Map<*, *> -> {
            val mapLikeSerializer = des as MapLikeSerializer<*, *, *, *>
            val obj = txn.newEntity("${ent.type}:$tag:map")
            value.entries.mapIndexed { index, (key, vl) ->
                parseElement(mapLikeSerializer.keySerializer, key, obj, "k$index", "${ent.type}:$tag:map:key")
                parseElement(
                    mapLikeSerializer.valueSerializer,
                    vl,
                    obj,
                    "v$index",
                    "${ent.type}:$tag:map:value"
                )
            }
            obj.setProperty(SIZE_PROPERTY_NAME, value.size)
            ent.setLink(tag, obj)
        }
        is Collection<*> -> value.filterNotNull().let { collection ->
            val elementDescriptor = des.descriptor.getElementDescriptor(0)
            val elementSerializer = elementDescriptor.takeIf { it.kind !is PrimitiveKind }?.run {
                classLoader.loadClass(serialName).kotlin.serializer()
            }
            if (elementSerializer is GeneratedSerializer) {
                collection.forEach {
                    val obj = txn.newEntity(it::class.simpleName.toString())
                    val serializer: KSerializer<Any> = unchecked(it::class.serializer())
                    XodusEncoder(txn, classLoader, obj).encodeSerializableValue(serializer, it)
                    ent.addLink(tag, obj)
                }
            } else {
                val obj = txn.newEntity(ent::class.simpleName.toString() + "list")
                obj.setProperty(SIZE_PROPERTY_NAME, collection.size)
                collection.forEachIndexed { i, it ->
                    obj.setProperty(i.toString(), it as Comparable<*>)
                }
                ent.setLink(tag, obj)
            }
        }
        is Enum<*> -> {
            ent.setProperty(tag, value.ordinal)
        }

        else -> {
            if (isId) {
                ent.setProperty(tag, value.encodeId())
            } else {
                @Suppress("UNCHECKED_CAST")
                val strategy = value::class.serializer() as KSerializer<Any>
                val obj = txn.newEntity(value::class.simpleName.toString())
                XodusEncoder(txn, classLoader, obj).encodeSerializableValue(strategy, value)
                ent.setLink(tag, obj)
            }
        }
    }


    private fun streamSerialization(
        serializationSerializationSettings: SerializationSettings,
        ent: Entity,
        value: Any,
        tag: String,
    ) = when (serializationSerializationSettings.serializationType) {
        SerializationType.FST -> {
            val conf = FSTConfiguration.getDefaultConfiguration()
            val path = "${ent.store.location}\\${ent.type.replace(":", "\\")}"
            Files.createDirectories(Paths.get(path))
            val file = File(path, "${UUID.randomUUID()}.bin")
            when (serializationSerializationSettings.compressType) {
                CompressType.ZSTD -> ZstdCompressorOutputStream(file.outputStream())
                else -> file.outputStream()
            }.use {
                logger.trace { "Saving entity: ${ent.type} to file: $path" }
                conf.encodeToStream(it, value)
                ent.setProperty(tag, file.absolutePath)
            }
        }
    }


    private fun parseElement(
        targetSerializer: KSerializer<out Any?>,
        property: Any?,
        obj: Entity,
        keyName: String,
        tag: String,
    ) {
        if (targetSerializer !is GeneratedSerializer<*>) {
            when (property) {
                is Enum<*> -> obj.setProperty(keyName, property.ordinal)
                is Comparable<*> -> obj.setProperty(keyName, property)
                else -> storeObject(unchecked(targetSerializer), false, null, property!!, obj, keyName)
            }
        } else {
            val mapKey = txn.newEntity(tag)
            XodusEncoder(txn, classLoader, mapKey).encodeSerializableValue(unchecked(targetSerializer), property!!)
            obj.setLink(keyName, mapKey)
        }
    }


    private fun encodeElement(descriptor: SerialDescriptor, index: Int) = pushTag(descriptor.getTag(index))

    override fun encodeNotNullMark() = Unit
    override fun encodeNull() = encodeTaggedNull(popTag())

    override fun encodeBoolean(value: Boolean) = encodeTaggedBoolean(popTag(), value)
    override fun encodeByte(value: Byte) = encodeTaggedByte(popTag(), value)
    override fun encodeShort(value: Short) = encodeTaggedShort(popTag(), value)
    override fun encodeInt(value: Int) = encodeTaggedInt(popTag(), value)
    override fun encodeLong(value: Long) = encodeTaggedLong(popTag(), value)
    override fun encodeFloat(value: Float) = encodeTaggedFloat(popTag(), value)
    override fun encodeDouble(value: Double) = encodeTaggedDouble(popTag(), value)
    override fun encodeChar(value: Char) = encodeTaggedChar(popTag(), value)
    override fun encodeString(value: String) = encodeTaggedString(popTag(), value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {}

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = this

    override fun endStructure(descriptor: SerialDescriptor) {
        if (tagStack.isNotEmpty()) popTag()
    }


    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) =
        encodeTaggedBoolean(descriptor.getTag(index), value)

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) =
        encodeTaggedByte(descriptor.getTag(index), value)

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) =
        encodeTaggedShort(descriptor.getTag(index), value)

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) =
        encodeTaggedInt(descriptor.getTag(index), value)

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) =
        encodeTaggedLong(descriptor.getTag(index), value)

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) =
        encodeTaggedFloat(descriptor.getTag(index), value)

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) =
        encodeTaggedDouble(descriptor.getTag(index), value)

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) =
        encodeTaggedChar(descriptor.getTag(index), value)

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) =
        encodeTaggedString(descriptor.getTag(index), value)

    override fun <T : Any?> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
    ) {
        encodeElement(descriptor, index)
        encodeTaggedObject(
            descriptor.getTag(index),
            value as Any,
            descriptor.getElementAnnotations(index).any { it is Id },
            getSerializationSettings(descriptor.getElementAnnotations(index)),
            unchecked(serializer)
        )
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?,
    ) {
        encodeElement(descriptor, index)
        if (serializer is GeneratedSerializer)
            encodeTaggedObject(
                descriptor.getTag(index),
                value as Any,
                descriptor.getElementAnnotations(index).any { it is Id },
                getSerializationSettings(descriptor.getElementAnnotations(index)),
                unchecked(serializer)
            )
        else
            encodeNullableSerializableValue(serializer, value)
    }

    private val tagStack = arrayListOf<String>()

    private fun pushTag(name: String) {
        tagStack.add(name)
    }

    private fun popTag() =
        if (tagStack.isNotEmpty())
            tagStack.removeAt(tagStack.lastIndex)
        else
            throw SerializationException("No tag in stack for requested element")
}
