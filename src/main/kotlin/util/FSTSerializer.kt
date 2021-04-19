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

import org.nustaq.serialization.*
import org.nustaq.serialization.coders.*

/*
val fst: FSTConfiguration = FSTConfiguration.createDefaultConfiguration().also {
    it.streamCoderFactory = StreamDecoderFactory(it)
    it.isForceSerializable = true //TODO check for perf
    it.isShareReferences = false //TODO check for perf
}
 */

internal class StreamDecoderFactory(
    private val fstConfiguration: FSTConfiguration,
    private val input: ThreadLocal<*> = ThreadLocal<Any?>(),
    private val output: ThreadLocal<*> = ThreadLocal<Any?>(),
) : FSTConfiguration.StreamCoderFactory {

    override fun createStreamEncoder(): FSTEncoder {
        return FSTStreamEncoder(fstConfiguration)
    }

    override fun createStreamDecoder(): FSTDecoder {
        return FSTCustomSteamDecoder(fstConfiguration)
    }

    override fun getInput(): ThreadLocal<*> {
        return input
    }

    override fun getOutput(): ThreadLocal<*> {
        return output
    }
}

internal class FSTCustomSteamDecoder(conf: FSTConfiguration) : FSTStreamDecoder(conf) {
    override fun readStringUTF(): String {
        return super.readStringUTF().weakIntern()
    }
}
