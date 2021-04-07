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
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlin.reflect.full.*

const val SIZE_PROPERTY_NAME = "size"

val json = Json {
    allowStructuredMapKeys = true
}

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
