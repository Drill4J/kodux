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
import org.nustaq.serialization.*
import org.nustaq.serialization.coders.*
import java.io.*


val fst: FSTConfiguration = FSTCustomConfiguration().also {
    it.streamCoderFactory = StreamDecoderFactory(it)
//    it.isForceSerializable = true //TODO EPMDJ-6919 check for perf
//    it.isShareReferences = false //TODO EPMDJ-6919 check for perf
}




internal class StreamDecoderFactory(
    private val fstConfiguration: FSTConfiguration,
) : FSTConfiguration.StreamCoderFactory {

    companion object {
        private val inputStreamThreadLocal: ThreadLocal<*> = ThreadLocal<Any?>()
        private val outputStreamThreadLocal: ThreadLocal<*> = ThreadLocal<Any?>()
    }

    override fun createStreamEncoder(): FSTEncoder {
        return FSTStreamEncoder(fstConfiguration)
    }

    override fun createStreamDecoder(): FSTDecoder {
        return object : FSTStreamDecoder(fstConfiguration) {
            override fun readStringUTF(): String {
                return super.readStringUTF().weakIntern()
            }

            override fun readStringAsc(): String {
                return super.readStringAsc().weakIntern()
            }
        }
    }

    override fun getInput(): ThreadLocal<*> {
        return inputStreamThreadLocal
    }

    override fun getOutput(): ThreadLocal<*> {
        return outputStreamThreadLocal
    }
}

internal class FSTCustomConfiguration : FSTConfiguration(null) {

    init {
        initDefaultFstConfigurationInternal(this)
    }

    override fun encodeToStream(out: OutputStream, toSerialize: Any) {
        super.encodeToStream(out, toSerialize)
        getObjectOutput(byteArrayOf())
    }

    override fun decodeFromStream(inputStream: InputStream): Any {
        return super.decodeFromStream(inputStream).also {
            getObjectInput(byteArrayOf())
        }
    }
}
