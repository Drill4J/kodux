@file:Suppress("UNCHECKED_CAST")

package com.epam.kodux

import jetbrains.exodus.bindings.*
import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*


class XodusEncoder(private val txn: StoreTransaction, private val ent: Entity) : Encoder, CompositeEncoder {
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
        storeObject(des, value, ent, tag)
    }

    private fun storeObject(des: SerializationStrategy<Any>, value: Any, ent: Entity, tag: String) {
        when (des) {
            is ListLikeSerializer<*, *, *> -> {
                val deserializer = des.typeParams.first() as KSerializer<Any>
                check(value is Collection<*>)
                when (des) {
                    is ArrayListSerializer<*>,
                    is HashSetSerializer<*>,
                    is LinkedHashSetSerializer<*> -> {
                        val filterNotNull = value.filterNotNull()
                        filterNotNull.forEach {
                            if (deserializer is GeneratedSerializer) {
                                val obj = txn.newEntity(it::class.simpleName.toString())
                                XodusEncoder(txn, obj).encode(deserializer, it)
                                ent.addLink(tag, obj)
                            } else {
                                ent.setProperty(tag, ComparableSet(filterNotNull as Collection<Comparable<Any>>))
                            }
                            Unit
                        }
                    }
                    is ReferenceArraySerializer<*, *> -> {
                        TODO("array serializer s not implemented yet")
                    }
                }
            }
            is EnumSerializer<*> -> {
                check(value is Enum<*>)
                ent.setProperty(tag, value.ordinal)
            }
            is MapLikeSerializer<*, *, *, *> -> {
                val map = value as Map<*, *>
                val obj = txn.newEntity("${ent.type}:$tag:map")
                map.entries.mapIndexed { index, (key, vl) ->
                    parseElement(des.keySerializer, key, obj, "k$index", "${ent.type}:$tag:map:key")
                    parseElement(des.valueSerializer, vl, obj, "v$index", "${ent.type}:$tag:map:value")
                }
                obj.setProperty(SIZE_PROPERTY_NAME, map.size)
                ent.setLink(tag, obj)
            }
            else -> {
                val obj = txn.newEntity(value::class.simpleName.toString())
                @Suppress("UNCHECKED_CAST") val strategy = value::class.serializer() as KSerializer<Any>
                XodusEncoder(txn, obj).encode(strategy, value)
                ent.setLink(tag, obj)
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
            if ((property is Comparable<*>))
                obj.setProperty(keyName, property)
            else {
                storeObject(targetSerializer as KSerializer<Any>, property!!, obj, keyName)
            }
        } else {
            val mapKey = txn.newEntity(tag)
            XodusEncoder(txn, mapKey).encode(targetSerializer as GeneratedSerializer<Any>, property!!)
            obj.setLink(keyName, mapKey)
        }
    }


    private fun encodeElement(desc: SerialDescriptor, index: Int) = pushTag(desc.getTag(index))

    override fun encodeNotNullMark() = TODO("not implemented yet")
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

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) = TODO("not implemented yet")

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return this
    }

    override fun endStructure(desc: SerialDescriptor) {
        if (tagStack.isNotEmpty()) popTag()
    }


    override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) =
            TODO("not implemented yet")

    override fun encodeUnitElement(desc: SerialDescriptor, index: Int) = TODO("not implemented yet")
    override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) =
            encodeTaggedBoolean(desc.getTag(index), value)

    override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) =
            encodeTaggedByte(desc.getTag(index), value)

    override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) =
            encodeTaggedShort(desc.getTag(index), value)

    override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) =
            encodeTaggedInt(desc.getTag(index), value)

    override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) =
            encodeTaggedLong(desc.getTag(index), value)

    override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) =
            encodeTaggedFloat(desc.getTag(index), value)

    override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) =
            encodeTaggedDouble(desc.getTag(index), value)

    override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) =
            encodeTaggedChar(desc.getTag(index), value)

    override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) =
            encodeTaggedString(desc.getTag(index), value)

    override fun <T : Any?> encodeSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T
    ) {
        encodeElement(desc, index)
        encodeTaggedObject(
                desc.getTag(index),
                value as Any,
                desc.getElementAnnotations(index).firstOrNull() is Id,
                serializer as SerializationStrategy<Any>
        )
    }

    override fun <T : Any> encodeNullableSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
    ) {
        encodeElement(desc, index)
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