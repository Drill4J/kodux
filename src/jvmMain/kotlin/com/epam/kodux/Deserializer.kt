@file:Suppress("UNCHECKED_CAST")

package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*


class XodusDecoder(private val txn: StoreTransaction, private val ent: Entity) : Decoder, CompositeDecoder {
    override val context: SerialModule
        get() = EmptyModule

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
        return when (des) {
            is EnumSerializer -> this.decode(des)
            is ListLikeSerializer<*, *, *> -> {
                val deserializer = des.typeParams.first()
                val objects =
                        if (deserializer is GeneratedSerializer<*>)
                            ent.getLinks(tag).map { XodusDecoder(txn, it).decode(deserializer) }.asIterable()
                        else {
                            val link = ent.getLink(tag)!!
                            val size = link.getProperty("size") as Int
                            val map = (0 until size).map {
                                link.getProperty(it.toString()) as Comparable<*>
                            }
                            map
                        }
                parseListBasedObject(des, objects)
            }
            is MapLikeSerializer<*, *, *, *> -> {
                val link = checkNotNull(ent.getLink(tag)) { "nullable collections are not supported yet" }
                val size = link.getProperty(SIZE_PROPERTY_NAME) as Int
                val associateWith = (0 until size).associate {
                    parseElement(des.keySerializer, link, "k$it") to parseElement(des.valueSerializer, link, "v$it")
                }
                associateWith as T
            }
            else -> {
                XodusDecoder(txn, checkNotNull(ent.getLink(tag)) { "should be not null" }).decode(des)
            }
        }
    }

    private fun parseElement(targetSerializer: KSerializer<out Any?>, link: Entity, propertyName: String) =
            if (targetSerializer !is GeneratedSerializer<*>) {
                link.getProperty(propertyName) ?: restoreObject(targetSerializer, link, propertyName)

            } else
                XodusDecoder(txn, link.getLink(propertyName)!!).decode(targetSerializer)

    private fun <T> parseListBasedObject(des: ListLikeSerializer<*, *, *>, objects: Iterable<Any?>): T {
        return when (des) {
            is ArrayListSerializer<*> -> objects.toMutableList() as T
            is HashSetSerializer<*> -> objects.toMutableSet() as T
            is LinkedHashSetSerializer<*> -> objects.toMutableSet() as T
            is ReferenceArraySerializer<*, *> -> TODO("not implemented yet")
            is PrimitiveArraySerializer<*, *, *> -> TODO("not implemented yet")
        }
    }


    override fun decodeNotNullMark(): Boolean = decodeTaggedNotNullMark(currentTag)
    override fun decodeNull(): Nothing? = null

    override fun decodeUnit() = TODO("not implemented yet")
    override fun decodeBoolean(): Boolean = decodeTaggedBoolean(popTag())
    override fun decodeByte(): Byte = decodeTaggedByte(popTag())
    override fun decodeShort(): Short = decodeTaggedShort(popTag())
    override fun decodeInt(): Int = decodeTaggedInt(popTag())
    override fun decodeLong(): Long = decodeTaggedLong(popTag())
    override fun decodeFloat(): Float = decodeTaggedFloat(popTag())
    override fun decodeDouble(): Double = decodeTaggedDouble(popTag())
    override fun decodeChar(): Char = decodeTaggedChar(popTag())
    override fun decodeString(): String = decodeTaggedString(popTag())

    override fun decodeEnum(enumDescription: EnumDescriptor): Int = decodeTaggedEnum(popTag())

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return this
    }

    /**
     * Assumes that all elements go in order by default.
     */
    override fun decodeElementIndex(desc: SerialDescriptor): Int = CompositeDecoder.READ_ALL

    override fun decodeUnitElement(desc: SerialDescriptor, index: Int) = TODO("not implemented yet")
    override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean =
            decodeTaggedBoolean(desc.getTag(index))

    override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte =
            decodeTaggedByte(desc.getTag(index))

    override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short =
            decodeTaggedShort(desc.getTag(index))

    override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int = decodeTaggedInt(desc.getTag(index))
    override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long =
            decodeTaggedLong(desc.getTag(index))

    override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float =
            decodeTaggedFloat(desc.getTag(index))

    override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double =
            decodeTaggedDouble(desc.getTag(index))

    override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char =
            decodeTaggedChar(desc.getTag(index))

    override fun decodeStringElement(desc: SerialDescriptor, index: Int): String =
            decodeTaggedString(desc.getTag(index))

    override fun <T : Any?> decodeSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>
    ): T =
            tagBlock(desc.getTag(index)) {

                decodeTaggedObject(desc.getTag(index), deserializer)
            }

    override fun <T : Any> decodeNullableSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>
    ): T? =
            tagBlock(desc.getTag(index)) {
                if (deserializer is GeneratedSerializer)
                    decodeTaggedObject(desc.getTag(index), deserializer)
                else
                    decodeNullableSerializableValue(deserializer)

            }

    override fun <T> updateSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T>,
            old: T
    ): T =
            tagBlock(desc.getTag(index)) { updateSerializableValue(deserializer, old) }

    override fun <T : Any> updateNullableSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            deserializer: DeserializationStrategy<T?>,
            old: T?
    ): T? =
            tagBlock(desc.getTag(index)) { updateNullableSerializableValue(deserializer, old) }

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
}