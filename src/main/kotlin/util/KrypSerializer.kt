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
package com.epam.kodux.util

import com.esotericsoftware.kryo.*
import com.esotericsoftware.kryo.io.*
import com.esotericsoftware.kryo.serializers.*
import com.esotericsoftware.kryo.util.*
import java.io.*
import java.lang.ref.*
import java.util.*

private val kryoPool: Pool<Kryo> = object : Pool<Kryo>(true, true, 8) {
    override fun create(): Kryo = Kryo().also {
        it.isRegistrationRequired = false
        it.register(LinkedHashMap<Any, Any>().keys::class.java, SetSerializer(it.getSerializer(HashSet::class.java)))
    }
}

internal inline fun <T> kryo(classLoader: ClassLoader, block: Kryo.(ClassLoader) -> T): T = kryoPool.run {
    obtain().let { kryo ->
        try {
            kryo.classLoader = classLoader
            block(kryo, classLoader)
        } finally {
            free(kryo)
        }
    }
}

internal class CustomInput(inputStream: InputStream) : Input(inputStream) {
    override fun readString(): String {
        return super.readString().weakIntern()
    }
}

internal class CustomStringSerializer : DefaultSerializers.StringSerializer() {
    override fun read(kryo: Kryo?, input: Input?, type: Class<out String>?): String {
        return super.read(kryo, input, type).weakIntern()
    }
}


class CustomSerializer<T>(private val defaultSerializer: Serializer<Any>) : Serializer<T>() {

    override fun write(kryo: Kryo, output: Output, `object`: T) {
        defaultSerializer.write(kryo, output, `object`)
    }

    @Suppress("UNCHECKED_CAST")
    override fun read(kryo: Kryo, input: Input, type: Class<out T>): T {
        return (defaultSerializer.read(kryo, input, type) as T).weakIntern()
    }

    private val weakRefObjectPool = WeakHashMap<T, WeakReference<T>>()

    private fun T.weakIntern(): T {
        val cached = weakRefObjectPool[this]
        if (cached != null) {
            val value = cached.get()
            if (value != null) return value
        }
        weakRefObjectPool[this] = WeakReference(this)
        return this
    }
}

fun <T> Kryo.customSerializer(type: Class<T>) = CustomSerializer<T>(getDefaultSerializer(type))


class SetSerializer(private val defaultSerializer: Serializer<Any>) : Serializer<Set<*>>() {

    override fun write(kryo: Kryo?, output: Output?, `object`: Set<*>?) {
        defaultSerializer.write(kryo, output, HashSet(`object`))
    }

    override fun read(kryo: Kryo?, input: Input?, type: Class<out Set<*>>?): Set<*> {
        return defaultSerializer.read(kryo, input, HashSet::class.java) as Set<*>
    }

}

