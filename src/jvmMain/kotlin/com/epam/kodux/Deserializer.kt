package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import org.slf4j.*

val decoderLogger: Logger = LoggerFactory.getLogger("Decoder")

class XodusDecoder(private val txn: StoreTransaction, private val ent: Entity) : Decoder, CompositeDecoder {
    override val context: SerialModule
        get() = EmptyModule

    override val updateMode: UpdateMode = UpdateMode.UPDATE
    private fun SerialDescriptor.getTag(index: Int): String {
        decoderLogger.debug("Getting tag by index: $index (entity: $ent)")
        return this.getElementName(index)
    }

    private fun decodeTaggedBoolean(tag: String): Boolean {
        decoderLogger.debug("Decoding a Boolean by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Boolean
    }

    private fun decodeTaggedByte(tag: String): Byte {
        decoderLogger.debug("Decoding a Byte by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Byte
    }

    private fun decodeTaggedChar(tag: String): Char {
        decoderLogger.debug("Decoding a Char by tag: $tag (entity: $ent)")
        return ent.getProperty(tag).toString()[0]
    }

    private fun decodeTaggedDouble(tag: String): Double {
        decoderLogger.debug("Decoding a Double by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Double
    }

    private fun decodeTaggedEnum(tag: String): Int {
        decoderLogger.debug("Decoding an Enum by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Int
    }

    private fun decodeTaggedFloat(tag: String): Float {
        decoderLogger.debug("Decoding a Float by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Float
    }

    private fun decodeTaggedInt(tag: String): Int {
        decoderLogger.debug("Decoding an Int by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Int
    }

    private fun decodeTaggedLong(tag: String): Long {
        decoderLogger.debug("Decoding a Long by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Long
    }

    private fun decodeTaggedNotNullMark(@Suppress("UNUSED_PARAMETER") tag: String): Boolean {
        decoderLogger.debug("Decoding a not null mark by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) != null || ent.getLink(tag) != null
    }

    private fun decodeTaggedShort(tag: String): Short {
        decoderLogger.debug("Decoding a Short by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as Short
    }

    private fun decodeTaggedString(tag: String): String {
        decoderLogger.debug("Decoding a String by tag: $tag (entity: $ent)")
        return ent.getProperty(tag) as String
    }

    private fun <T> decodeTaggedObject(tag: String, des: DeserializationStrategy<T>): T {
        decoderLogger.debug("Decoding an Object by tag: $tag (entity: $ent)")
        return restoreObject(des, ent, tag)
    }

    private fun <T> restoreObject(des: DeserializationStrategy<T>, ent: Entity, tag: String): T {
        decoderLogger.debug("Restoring an object by tag: $tag (entity: $ent)")
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
                val list = parseListBasedObject(des, objects)
                unchecked(list)
            }
            is MapLikeSerializer<*, *, *, *> -> {
                val link = checkNotNull(ent.getLink(tag)) { "nullable collections are not supported yet" }
                val size = link.getProperty(SIZE_PROPERTY_NAME) as Int
                val associateWith = (0 until size).associate {
                    val k = parseElement(des.keySerializer, link, "k$it")
                    val v = parseElement(des.valueSerializer, link, "v$it")
                    k to v
                }
                unchecked(associateWith)
            }
            else -> {
                XodusDecoder(txn, checkNotNull(ent.getLink(tag)) { "should be not null" }).decode(des)
            }
        }
    }

    private fun parseElement(targetSerializer: KSerializer<out Any?>, link: Entity, propertyName: String) = run {
        decoderLogger.debug("Parsing a ${link.type}; contents: $link")
        if (targetSerializer !is GeneratedSerializer<*>) {
            link.getProperty(propertyName)
                ?: restoreObject(targetSerializer, link, propertyName)
        } else XodusDecoder(txn, link.getLink(propertyName)!!).decode(targetSerializer)
    }

    private fun parseListBasedObject(des: ListLikeSerializer<*, *, *>, objects: Iterable<Any?>): Any {
        return when (des) {
            is ArrayListSerializer<*> -> objects.toMutableList()
            is HashSetSerializer<*> -> objects.toMutableSet()
            is LinkedHashSetSerializer<*> -> objects.toMutableSet()
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