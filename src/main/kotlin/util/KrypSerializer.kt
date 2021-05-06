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


private val kryoPool: Pool<Kryo> = object : Pool<Kryo>(true, true, 8) {
    override fun create(): Kryo = Kryo().also {
        it.isRegistrationRequired = false
        it.register(String::class.java, CustomStringSerializer())
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
