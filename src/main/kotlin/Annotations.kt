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

import kotlinx.serialization.*
import kotlin.reflect.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Id

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class StreamSerialization(
    val serializationType: SerializationType,
    val compressType: CompressType,
    val poolRegistration: Array<KClass<*>> = [],
)

enum class SerializationType {
    FST, KRYO
}

enum class CompressType {
    NONE, ZSTD
}

internal fun getSerializationSettings(
    annotation: List<Annotation>,
) = annotation.firstOrNull { it is StreamSerialization }?.let {

    val customSerialization = it as StreamSerialization

    //raw hack for annotation processing issues.
    @Suppress("UNCHECKED_CAST")
    val poolRegistration: Array<KClass<*>> = customSerialization::class.java.declaredMethods.first { method ->
        method.name == customSerialization::poolRegistration.name
    }(customSerialization) as Array<KClass<*>>

    SerializationSettings(
        customSerialization.serializationType,
        customSerialization.compressType,
        poolRegistration
    )
}

internal class SerializationSettings(
    val serializationType: SerializationType,
    val compressType: CompressType,
    val poolRegistration: Array<KClass<*>>,
)
