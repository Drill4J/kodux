package com.epam.kodux.decoder

import com.epam.kodux.*
import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*


class XodusDecoder(private val txn: StoreTransaction, private val ent: Entity, val idName: String? = null) : Decoder, CompositeDecoder {
    override val serializersModule: SerializersModule = EmptySerializersModule

    override val updateMode: UpdateMode = UpdateMode.UPDATE
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

    private fun <T> decodeTaggedObject(tag: String, des: DeserializationStrategy<T>): T {
        return restoreObject(des, ent, tag)
    }

    private fun <T> restoreObject(des: DeserializationStrategy<T>, ent: Entity, tag: String): T {
        @Suppress("UNCHECKED_CAST")
        return when (des) {
            ByteArraySerializer() -> {
                ent.getBlob(tag)?.readBytes()
            }
            is AbstractCollectionSerializer<*, *, *> -> {
                val link = checkNotNull(ent.getLink(tag)) { "$tag nullable collections are not supported yet" }
                XodusDecoder(txn, link).decodeSerializableValue(des)
            }
            else -> if (tag == idName) decodeTaggedString(tag).decodeId(des) else
                when (des.descriptor.kind) {
                    SerialKind.ENUM -> decodeSerializableValue(des)
                    PrimitiveKind.BOOLEAN -> decodeTaggedBoolean(tag)
                    PrimitiveKind.BYTE -> decodeTaggedByte(tag)
                    PrimitiveKind.CHAR -> decodeTaggedChar(tag)
                    PrimitiveKind.INT -> decodeTaggedInt(tag)
                    PrimitiveKind.STRING -> decodeTaggedString(tag)
                    PrimitiveKind.FLOAT -> decodeTaggedFloat(tag)
                    PrimitiveKind.DOUBLE -> decodeTaggedDouble(tag)
                    PrimitiveKind.LONG -> decodeTaggedLong(tag)
                    PrimitiveKind.SHORT -> decodeTaggedShort(tag)
                    else -> XodusDecoder(
                        txn = txn,
                        ent = checkNotNull(ent.getLink(tag)) { "should be not null $tag" }
                    ).decodeSerializableValue(des)
                }
        } as T
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

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decodeTaggedInt(descriptor.getTag(index))
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

    override fun <T : Any?> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T = tagBlock(descriptor.getTag(index)) {
        decodeTaggedObject(descriptor.getTag(index), deserializer)
    }

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? = tagBlock(descriptor.getTag(index)) {
        if (deserializer is GeneratedSerializer)
            decodeTaggedObject(descriptor.getTag(index), deserializer)
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

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return when (descriptor.kind) {
            StructureKind.MAP -> ent.run { propertyNames.size + linkNames.size } / 2
            StructureKind.LIST -> ent.run { propertyNames.size + linkNames.size }
            else -> -1
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) = Unit
}
