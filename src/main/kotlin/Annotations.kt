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

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Id

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class StreamSerialization(
    val serializationType: SerializationType,
    val compressType: CompressType,
)

enum class SerializationType {
    FST
}

enum class CompressType {
    NONE, ZSTD
}

internal fun getSerializationSettings(
    annotation: List<Annotation>,
) = annotation.firstOrNull { it is StreamSerialization }?.let {
    val customSerialization = it as StreamSerialization
    SerializationSettings(customSerialization.serializationType, customSerialization.compressType)
}

internal class SerializationSettings(
    val serializationType: SerializationType,
    val compressType: CompressType,
)
