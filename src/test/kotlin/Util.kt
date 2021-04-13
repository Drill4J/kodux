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

import kotlin.test.*

suspend inline fun <reified T : Any> updateObject(
    data: T,
    id: Any,
    agentStore: StoreClient,
    objectCount: Int = 1,
) {
    agentStore.store(data)
    assertEquals(objectCount, agentStore.getAll<T>().count())
    assertEquals(data, agentStore.findById<T>(id))
}
