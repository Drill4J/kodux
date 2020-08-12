package com.epam.kodux.encoder

import com.epam.kodux.*
import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

class XodusEncoder(
    private val txn: StoreTransaction,
    private val ent: Entity
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

    private fun <T> encodeTaggedObject(tag: String, value: T, @Suppress("UNUSED_PARAMETER") isId: Boolean, des: SerializationStrategy<T>) {
        storeObject(des, isId, value, ent, tag)
    }

    private fun <T> storeObject(
        des: SerializationStrategy<T>,
        isId: Boolean,
        value: T,
        ent: Entity,
        tag: String
    ) {
        when (value) {
            null -> encodeTaggedNull(tag)
            is Boolean -> encodeTaggedBoolean(tag, value)
            is Byte -> encodeTaggedByte(tag, value)
            is Char -> encodeTaggedChar(tag, value)
            is Double -> encodeTaggedDouble(tag, value)
            is Float -> encodeTaggedFloat(tag, value)
            is Int -> encodeTaggedInt(tag, value)
            is Long -> encodeTaggedLong(tag, value)
            is String -> encodeTaggedString(tag, value)
            is Short -> encodeTaggedShort(tag, value)
            is ByteArray -> {
                ent.setBlob(tag, value.inputStream())
            }
            is Map<*, *> -> {
                val obj = txn.newEntity("${ent.type}:$tag:map")
                XodusEncoder(txn, obj).encodeSerializableValue(des, value)
                ent.setLink(tag, obj)
            }
            is Collection<*> -> {
                val obj = txn.newEntity("${ent.type}:$tag:collection")
                XodusEncoder(txn, obj).encodeSerializableValue(des, value)
                ent.setLink(tag, obj)
            }
            is Enum<*> -> {
                ent.setProperty(tag, value.ordinal)
            }
            else -> {
                if (isId) {
                    ent.setProperty(tag, value.encodeId())
                } else {
                    val obj = txn.newEntity((value as Any)::class.simpleName.toString())
                    XodusEncoder(txn, obj).encodeSerializableValue(des, value)
                    ent.setLink(tag, obj)
                }
            }
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

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return this
    }

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
        value: T
    ) {
        encodeElement(descriptor, index)
        encodeTaggedObject(
            descriptor.getTag(index),
            value,
            descriptor.getElementAnnotations(index).firstOrNull() is Id,
            unchecked(serializer)
        )
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        encodeElement(descriptor, index)
        if (serializer is GeneratedSerializer)
            encodeTaggedObject(
                descriptor.getTag(index),
                value,
                descriptor.getElementAnnotations(index).firstOrNull() is Id,
                unchecked(serializer))
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
