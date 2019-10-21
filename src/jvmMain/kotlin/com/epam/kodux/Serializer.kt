package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import org.slf4j.*

val encoderLogger: Logger = LoggerFactory.getLogger("Encoder")

class XodusEncoder(private val txn: StoreTransaction, private val ent: Entity) : Encoder, CompositeEncoder {
    private fun SerialDescriptor.getTag(index: Int) = this.getElementName(index)

    override val context: SerialModule
        get() = EmptyModule

    private fun encodeTaggedBoolean(tag: String, value: Boolean) {
        encoderLogger.debug("Encoding tagged Boolean with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedByte(tag: String, value: Byte) {
        encoderLogger.debug("Encoding tagged Byte with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedChar(tag: String, value: Char) {
        //doesn't support character
        encoderLogger.debug("Encoding tagged Char with tag: $tag (value: $value)")
        ent.setProperty(tag, value.toString())
    }

    private fun encodeTaggedDouble(tag: String, value: Double) {
        encoderLogger.debug("Encoding tagged Double with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedFloat(tag: String, value: Float) {
        encoderLogger.debug("Encoding tagged Float with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedInt(tag: String, value: Int) {
        encoderLogger.debug("Encoding tagged Int with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedLong(tag: String, value: Long) {
        encoderLogger.debug("Encoding tagged Long with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedNull(@Suppress("UNUSED_PARAMETER") tag: String) {
        encoderLogger.error("Tried to encode null with tag: $tag")
    }

    private fun encodeTaggedShort(tag: String, value: Short) {
        encoderLogger.debug("Encoding tagged Short with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedString(tag: String, value: String) {
        encoderLogger.debug("Encoding tagged String with tag: $tag (value: $value)")
        ent.setProperty(tag, value)
    }

    private fun encodeTaggedObject(tag: String, value: Any, @Suppress("UNUSED_PARAMETER") isId: Boolean, des: SerializationStrategy<Any>) {
        encoderLogger.debug("Encoding tagged Object with tag: $tag (value: $value)")
        storeObject(des, value, ent, tag)
    }

    private fun storeObject(des: SerializationStrategy<Any>, value: Any, ent: Entity, tag: String) {
        encoderLogger.info("Storing an object; tag: $tag, value: $value, entity: $ent")
        when (des) {
            is ListLikeSerializer<*, *, *> -> {
                val deserializer: KSerializer<Any> = unchecked(des.typeParams.first())
                encoderLogger.debug("Checking value $value is a Collection")
                check(value is Collection<*>)
                when (des) {
                    is ArrayListSerializer<*>,
                    is HashSetSerializer<*>,
                    is LinkedHashSetSerializer<*> -> {
                        encoderLogger.debug("Value $value is being filtered (filterNotNull)")
                        val filterNotNull = value.filterNotNull()
                        if (deserializer is GeneratedSerializer) {
                            filterNotNull.forEach {
                                encoderLogger.debug("Encoding and linking: $it")
                                val obj = txn.newEntity(it::class.simpleName.toString())
                                XodusEncoder(txn, obj).encode(deserializer, it)
                                ent.addLink(tag, obj)
                            }
                        } else {
                            encoderLogger.debug("Linking: $ent")
                            val obj = txn.newEntity(ent::class.simpleName.toString() + "list")
                            obj.setProperty("size", filterNotNull.size)
                            filterNotNull.forEachIndexed { i, it ->
                                obj.setProperty(i.toString(), it as Comparable<*>)
                            }
                            ent.setLink(tag, obj)
                        }
                    }
                    is ReferenceArraySerializer<*, *> -> {
                        TODO("array serializer s not implemented yet")
                    }
                }
            }
            is EnumSerializer<*> -> {
                encoderLogger.debug("Checking value $value is a Enum")
                check(value is Enum<*>)
                ent.setProperty(tag, value.ordinal)
            }
            is MapLikeSerializer<*, *, *, *> -> {
                encoderLogger.debug("Casting value $value to Map")
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
        encoderLogger.info("Parsing property $property for entity $obj")
        if (targetSerializer !is GeneratedSerializer<*>) {
            if ((property is Comparable<*>))
                obj.setProperty(keyName, property)
            else {
                storeObject(unchecked(targetSerializer), property!!, obj, keyName)
            }
        } else {
            val mapKey = txn.newEntity(tag)
            XodusEncoder(txn, mapKey).encode(unchecked(targetSerializer), property!!)
            obj.setLink(keyName, mapKey)
        }
    }


    private fun encodeElement(desc: SerialDescriptor, index: Int) = pushTag(desc.getTag(index))

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
        encoderLogger.info("Encoding an element: $value")
        encodeElement(desc, index)
        encodeTaggedObject(
                desc.getTag(index),
                value as Any,
                desc.getElementAnnotations(index).firstOrNull() is Id,
                unchecked(serializer)
        )
    }

    override fun <T : Any> encodeNullableSerializableElement(
            desc: SerialDescriptor,
            index: Int,
            serializer: SerializationStrategy<T>,
            value: T?
    ) {
        encoderLogger.info("Encoding a nullable element: $value")
        encodeElement(desc, index)
        if (serializer is GeneratedSerializer)
            encodeTaggedObject(
                    desc.getTag(index),
                    value as Any,
                    desc.getElementAnnotations(index).firstOrNull() is Id,
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