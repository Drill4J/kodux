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

import com.esotericsoftware.kryo.*
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.full.*

const val SIZE_PROPERTY_NAME = "size"

val json = Json {
    allowStructuredMapKeys = true
}

val kryo = Kryo()

inline fun <reified T : Any> idPair(any: T): Pair<String?, Any?> {
    val idName = idName(T::class.serializer().descriptor)
    return idName to (T::class.memberProperties.find { it.name == idName })?.getter?.invoke(any)
}

fun idName(desc: SerialDescriptor): String? = (0 until desc.elementsCount).firstOrNull { index ->
    desc.getElementAnnotations(index).any { it is Id }
}?.let { idIndex -> desc.getElementName(idIndex) }

fun fieldsAnnotatedByStreamSerialization(desc: SerialDescriptor): List<String> = ArrayList<String>().apply {
    (0 until desc.elementsCount).forEach { index ->
        val list = fieldsAnnotatedByStreamSerialization(desc.getElementDescriptor(index))
        desc.getElementAnnotations(index).forEach {
            if (it is StreamSerialization)
                add(desc.getElementName(index))
        }
        addAll(list)
    }
}

fun Any.encodeId(): String = this as? String ?: json.encodeToString(unchecked(this::class.serializer()), this)

fun <T> String.decodeId(deser: DeserializationStrategy<T>): T = json.decodeFromString(deser, this)

@Suppress("UNCHECKED_CAST")
fun <T> unchecked(any: Any) = any as T


@OptIn(ExperimentalStdlibApi::class)
fun <T : Any> registerInKryoRec(klass: KClass<T>, kryo: Kryo, instance: Any) {
    val props = klass.memberProperties.sortedBy { it.name }
    val propsByName = props.associateBy { it.name }
    val constructor = klass.primaryConstructor
    if (constructor != null) {
        for (param in constructor.parameters) {
            val kProperty = propsByName[param.name!!]!!
            when (param.type.javaType.typeName) {
                "int" -> "Do noting"
                "long" -> "Do noting"
                "short" -> "Do noting"
                "char" -> "Do noting"
                "byte" -> "Do noting"
                "double" -> "Do noting"
                "float" -> "Do noting"
                "java.lang.String" -> "Do noting"
                else -> {
                    @Suppress("UNCHECKED_CAST")
                    val instance2 = kProperty.get(instance as T)!!
                    kryo.register(instance2::class.java, ImmutableClassSerializer(instance2::class))
                    registerInKryoRec(instance2::class, kryo, instance2)
                }
            }
        }
    }


}

class ImmutableClassSerializer<T : Any>(val klass: KClass<T>) : Serializer<T>() {

    val props = klass.memberProperties.sortedBy { it.name }
    val propsByName = props.associateBy { it.name }
    val constructor = klass.primaryConstructor

    @ExperimentalStdlibApi
    override fun write(kryo: Kryo, output: Output, obj: T) {
        if (constructor == null) {
//            obj::class.java
//            kryo.register(obj::class.java, kryo.getDefaultSerializer(obj::class.java))
            when (obj) {
                is Collection<*> -> {
                    obj.forEach {
                        kryo.register(it!!::class.java,ImmutableClassSerializer(it::class))
                        kryo.writeClassAndObject(output, it)
                    }
                }
                else ->{

                }
            }

        } else {
            output.writeVarInt(constructor.parameters.size, true)
            output.writeInt(constructor.parameters.hashCode())
            for (param in constructor.parameters) {
                val kProperty = propsByName[param.name!!]!!
                when (param.type.javaType.typeName) {
                    "int" -> output.writeVarInt(kProperty.get(obj) as Int, true)
                    "long" -> output.writeVarLong(kProperty.get(obj) as Long, true)
                    "short" -> output.writeShort(kProperty.get(obj) as Int)
                    "char" -> output.writeChar(kProperty.get(obj) as Char)
                    "byte" -> output.writeByte(kProperty.get(obj) as Byte)
                    "double" -> output.writeDouble(kProperty.get(obj) as Double)
                    "float" -> output.writeFloat(kProperty.get(obj) as Float)
                    "java.lang.String" -> output.writeString(kProperty.get(obj) as String)
                    else -> {
                        val `object` = kProperty.get(obj)!!
                        kryo.register(`object`::class.java, ImmutableClassSerializer(`object`::class))
                        kryo.writeClassAndObject(output, kProperty.get(obj))
//                    } catch (e: Exception) {
//                        throw IllegalStateException("Failed to serialize ${param.name} in ${klass.qualifiedName} ${e.message}", e)
                    }
                }
            }
        }
    }

    @ExperimentalStdlibApi
    override fun read(kryo: Kryo, input: Input, type: Class<out T>): T {
        if (constructor == null) {
            kryo.register(type, kryo.getDefaultSerializer(type))
            return kryo.readObject(input, type)
        } else {
            val numFields = input.readVarInt(true)
            val fieldTypeHash = input.readInt()

            // A few quick checks for data evolution. Note that this is not guaranteed to catch every problem! But it's
            // good enough for a prototype.
            if (numFields != constructor.parameters.size)
                throw KryoException("Mismatch between number of constructor parameters and number of serialised fields for ${klass.qualifiedName} ($numFields vs ${constructor.parameters.size})")
            if (fieldTypeHash != constructor.parameters.hashCode())
                throw KryoException("Hashcode mismatch for parameter types for ${klass.qualifiedName}: unsupported type evolution has happened.")

            val args = arrayOfNulls<Any?>(numFields)
            var cursor = 0
            for (param in constructor.parameters) {
                args[cursor++] = when (param.type.javaType.typeName) {
                    "int" -> input.readVarInt(true)
                    "long" -> input.readVarLong(true)
                    "short" -> input.readShort()
                    "char" -> input.readChar()
                    "byte" -> input.readByte()
                    "double" -> input.readDouble()
                    "float" -> input.readFloat()
                    "java.lang.String" -> input.readString()
                    else -> kryo.readClassAndObject(input)
                }
            }
            // If the constructor throws an exception, pass it through instead of wrapping it.
            return try {
                constructor.call(*args)
            } catch (e: InvocationTargetException) {
                throw e.cause!!
            }
        }
    }
}


fun <T : Any> kryoRecursiveRegistration(klass: KClass<T>, kryo: Kryo) {
    klass.declaredMemberProperties.forEach {
        it.returnType
        kryo.register(it.returnType::class.java, kryo.getDefaultSerializer(it.returnType::class.java))
    }

//    (0 until desc.elementsCount).forEach { index ->
//        kryoRecursiveRegistration(desc.getElementDescriptor(index))
//        if (desc.getElementDescriptor(index).kind !is PrimitiveKind) {
//            desc.getElementDescriptor(index).
//        }
//    }
}
