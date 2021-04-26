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

import mu.*
import org.nustaq.serialization.util.*
import java.lang.ref.*
import java.util.*

private val logger = KotlinLogging.logger { }

val initialPoolCapacity = (System.getenv("DRILL_INITIAL_POOL_SIZE")?.toInt() ?: 1_000_000).also {
    logger.info { "Initial pool size is $it" }
}


internal val weakRefStringPool = WeakHashMap<String, WeakReference<String>>(initialPoolCapacity)

fun String.weakIntern(): String {
    val cached = weakRefStringPool[this]
    if (cached != null) {
        val value = cached.get()
        if (value != null) return value
    }
    weakRefStringPool[this] = WeakReference(this)
    return this
}

@Suppress("unused")
fun logPoolStats() {
    logger.info { "Count strings ${weakRefStringPool.size} in pool" }
}
