package com.epam.kodux.encoder

import com.epam.kodux.*
import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

class XodusEncoder(
    private val txn: StoreTransaction,
    private val ent: Entity
) : Encoder, CompositeEncoder {
    private fun SerialDescriptor.getTag(index: Int) = this.getElementName(index)

    override val context: SerialModule
        get() = EmptyModule

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

    private fun encodeTaggedObject(tag: String, value: Any, @Suppress("UNUSED_PARAMETER") isId: Boolean, des: SerializationStrategy<Any>) {
        storeObject(des, isId, value, ent, tag)
    }

    private fun storeObject(
        des: SerializationStrategy<*>,
        isId: Boolean,
        value: Any,
        ent: Entity,
        tag: String
    ) {
        when (value) {
            is ByteArray -> {
                ent.setBlob(tag, value.inputStream())
            }
            is Map<*, *> -> {
                val abstractCollectionSerializer = des as AbstractCollectionSerializer<*, *, *>
                val obj = txn.newEntity("${ent.type}:$tag:map")
                value.entries.mapIndexed { index, (key, vl) ->
                    parseElement(abstractCollectionSerializer.typeParams[0], key, obj, "k$index", "${ent.type}:$tag:map:key")
                    parseElement(abstractCollectionSerializer.typeParams[1], vl, obj, "v$index", "${ent.type}:$tag:map:value")
                }
                obj.setProperty(SIZE_PROPERTY_NAME, value.size)
                ent.setLink(tag, obj)
            }
            is Collection<*> -> {
                val abstractCollectionSerializer = des as AbstractCollectionSerializer<*, *, *>
                val deserializer: KSerializer<Any> = unchecked(abstractCollectionSerializer.typeParams.first())
                val filterNotNull = value.filterNotNull()
                if (deserializer is GeneratedSerializer) {
                    filterNotNull.forEach {
                        val obj = txn.newEntity(it::class.simpleName.toString())
                        XodusEncoder(txn, obj).encode(deserializer, it)
                        ent.addLink(tag, obj)
                    }
                } else {
                    val obj = txn.newEntity(ent::class.simpleName.toString() + "list")
                    obj.setProperty("size", filterNotNull.size)
                    filterNotNull.forEachIndexed { i, it ->
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
                    XodusEncoder(txn, obj).encode(strategy, value)
                    ent.setLink(tag, obj)
                }
            }
        }
    }

    private fun parseElement(
            targetSerializer: KSerializer<out Any?>,
            property: Any?,
            obj: Entity,
            keyName: String,
            tag: String
    ) {
        if (targetSerializer !is GeneratedSerializer<*>) {
            when (property) {
                is Enum<*> -> obj.setProperty(keyName, property.ordinal)
                is Comparable<*> -> obj.setProperty(keyName, property)
                else -> storeObject(unchecked(targetSerializer), false, property!!, obj, keyName)
            }
        } else {
            val mapKey = txn.newEntity(tag)
            XodusEncoder(txn, mapKey).encode(unchecked(targetSerializer), property!!)
            obj.setLink(keyName, mapKey)
        }
    }


    private fun encodeElement(descriptor: SerialDescriptor, index: Int) = pushTag(descriptor.getTag(index))

    override fun encodeNotNullMark() = Unit
    override fun encodeNull() = encodeTaggedNull(popTag())

    override fun encodeUnit() = TODO("not implemented yet")
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

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (tagStack.isNotEmpty()) popTag()
    }


    override fun encodeNonSerializableElement(descriptor: SerialDescriptor, index: Int, value: Any) =
            TODO("not implemented yet")

    override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) = TODO("not implemented yet")
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
                value as Any,
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
                    value as Any,
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
