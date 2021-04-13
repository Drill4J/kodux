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

import com.epam.kodux.util.*
import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*
import org.junit.Test
import java.io.*
import java.util.*
import kotlin.test.*

class FstTest {

    private val storageDir = File("build/tmp/test/storages/${this::class.simpleName}-${UUID.randomUUID()}")
    private val agentStore = StoreClient(PersistentEntityStores.newInstance(storageDir))
    private val pathToEntity = "\\${MapInMapWrapper::class.simpleName}\\map\\map\\value"

    @AfterTest
    fun after() {
        agentStore.close()
        storageDir.deleteRecursively()
    }

    @Test
    fun `should correctly create,update,get and delete an object with id and StreamSerialization annotation`() =
        runBlocking {
            val id = CompositeId("one", 1)
            updateObject(StreamSerializationTestObject(id, listOf("one", "two", "three")), id, agentStore)
            updateObject(StreamSerializationTestObject(id, listOf("three", "two", "one")), id, agentStore)
            agentStore.deleteById<StreamSerializationTestObject>(id)
            assertEquals(0, agentStore.getAll<StreamSerializationTestObject>().count())
        }

    @Test
    fun `should not delete files belonging to another entity`() = runBlocking {
        val firstId = CompositeId("one", 1)
        val mapInMapWrapper = storeMapInMapWrapper(firstId)

        val secondId = CompositeId("two", 2)
        val mapInMapWrapper2 = storeMapInMapWrapper(secondId)

        assertEquals(
            10,
            File("${agentStore.location}$pathToEntity").listFiles()?.size ?: 0
        )
        assertEquals(mapInMapWrapper, agentStore.findById<MapInMapWrapper>(firstId))
        agentStore.deleteById<MapInMapWrapper>(firstId)
        assertEquals(
            5,
            File("${agentStore.location}$pathToEntity").listFiles()?.size ?: 0
        )
        assertEquals(mapInMapWrapper2, agentStore.findById<MapInMapWrapper>(secondId))
        agentStore.deleteById<MapInMapWrapper>(secondId)
        assertEquals(
            0,
            File("${agentStore.location}$pathToEntity").listFiles()?.size ?: 1
        )
    }


    @Test
    fun `should correctly delete an object with StreamSerialization annotation in inner object`() = runBlocking {
        val id = CompositeId("one", 1)
        val mapInMapWrapper = MapInMapWrapper(id, mutableMapOf())
        for (i in 1..5) {
            val a = MapWrapper(i, mutableMapOf())
            for (j in 1..5) {
                a.secondMap["$j"] = TestClass("$j", j)
            }
            mapInMapWrapper.map["$i"] = a
        }
        agentStore.store(mapInMapWrapper)
        assertEquals(mapInMapWrapper, agentStore.findById<MapInMapWrapper>(id))
        agentStore.deleteById<MapInMapWrapper>(id)
        assertTrue(File("${agentStore.location}$pathToEntity").listFiles().isNullOrEmpty())
    }

    @Test
    fun `deserialized string must be in string pool `() = runBlocking {
        val id = CompositeId("one", 1)
        val list = listOf("one".weakIntern(), "two".weakIntern(), "three".weakIntern())
        agentStore.store(StreamSerializationTestObject(id, list))
        val streamSerializationTestObject = agentStore.findById<StreamSerializationTestObject>(id)
        for (i in list.indices) {
            assertSame(list[i], streamSerializationTestObject!!.list[i])
        }
    }


    private suspend fun storeMapInMapWrapper(id: CompositeId): MapInMapWrapper {
        val mapInMapWrapper = MapInMapWrapper(id, mutableMapOf())
        for (i in 1..5) {
            val a = MapWrapper(i, mutableMapOf())
            for (j in 1..5) {
                a.secondMap["$j"] = TestClass("$j", j)
            }
            mapInMapWrapper.map["$i"] = a
        }
        agentStore.store(mapInMapWrapper)
        return mapInMapWrapper
    }

}
