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
import kotlin.random.Random
import kotlin.test.*

class FstTest {

    private val storageDir = File("build/tmp/test/storages/${this::class.simpleName}-${UUID.randomUUID()}")
    private val agentStore = StoreClient(PersistentEntityStores.newInstance(storageDir))
    private val pathToEntity = "\\${MapInMapWrapper::class.simpleName}\\map\\map\\value"

    @AfterTest
    fun after() {
        agentStore.close()
        storageDir.deleteRecursively()
        weakRefStringPool.clear()
    }

    @Test
    fun `should correctly create,update,get and delete an object with id and StreamSerialization annotation`() =
        runBlocking {
            val id = CompositeId("one", 1)
            updateObject(FSTSerializationTestObject(id, listOf("one", "two", "three")), id, agentStore)
            updateObject(FSTSerializationTestObject(id, listOf("three", "two", "one")), id, agentStore)
            agentStore.deleteById<FSTSerializationTestObject>(id)
            assertEquals(0, agentStore.getAll<FSTSerializationTestObject>().count())
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
        agentStore.store(FSTSerializationTestObject(id, list))
        val streamSerializationTestObject = agentStore.findById<FSTSerializationTestObject>(id)
        for (i in list.indices) {
            assertSame(list[i], streamSerializationTestObject!!.list[i])
        }
    }

    @Test
    fun `check weak pool`() = runBlocking {
        "first".weakIntern()
        "first".weakIntern()
        assertEquals(1, weakRefStringPool.size)
    }

    @Test
    fun `perf test! store and load a lot of data`() = runBlocking {
        val countSessions = 10
        repeat(countSessions) { index ->
            index.takeIf { it % 10 == 0 }?.let { println("store session, index = $it...") }
            startStopSession("sessionId$index")
        }
        repeat(countSessions) { index ->
            index.takeIf { it % 10 == 0 }?.let { println("load session, index = $it...") }
            loadSession("sessionId$index")
        }
    }

    @Test
    fun `perf test! store and load big data`() = runBlocking {
        val sessionId = "sessionId"
        //ok
        startStopSession(sessionId, 1, 1_000, 20_000)
        //oom
        //startStopSession(sessionId, 1, 5_000, 20_000)
        loadSession(sessionId)
    }

    @Test
    @Ignore("Test for local run only, set the heap size to 2g before running")
    fun `memory consumption test`(): Unit = runBlocking(Dispatchers.IO) {
        val sessionId = "sessionId"

        startStopSession(sessionId, 1, 11_650, 20_000)

        Thread.sleep(1000)

        loadSession(sessionId)

        Thread.sleep(1000)
        val firstId = CompositeId("one", 1)

        storeMapInMapWrapper(firstId, 1220)

        Thread.sleep(1000)

        loadSession(sessionId)

        Thread.sleep(1000)

        println("Loading Loading map in map")
        agentStore.findById<MapInMapWrapper>(firstId)
        Thread.sleep(1000)

        startStopSession(sessionId, 1, 11_650, 20_000)
    }


    private suspend fun loadSession(s: String) {
        println("Loading session")
        val finishSession = agentStore.findById<FinishSession>(s)
        println(finishSession?.execClassData?.size ?: 0)
    }

    private suspend fun startStopSession(
        sessionId: String,
        countAddProbes: Int = 1,
        sizeExec: Int = 1_000,
        sizeProbes: Int = 20_000,
    ) {
        println("Storing session")
        var collection: List<ExecClassData> = mutableListOf()
        repeat(countAddProbes) { index ->
            index.takeIf { it % 10 == 0 }?.let { println("adding probes, index = $it...") }
            val execClassData: List<ExecClassData> = listOf(0 until sizeExec).flatten().map {
                ExecClassData(
                    id = Random.nextLong(100_000_000),
                    className = "foo/Bar",
                    probes = randomBoolean(sizeProbes)
                )
            }
            collection = execClassData
        }
        agentStore.store(FinishSession(sessionId, collection))
        println("Session stored")
    }

    private fun randomBoolean(n: Int = 100) = listOf(0 until n).flatten().map { true }

    private suspend fun storeMapInMapWrapper(id: CompositeId, size: Int = 5): MapInMapWrapper {
        println("Storing map in map")
        val mapInMapWrapper = MapInMapWrapper(id, mutableMapOf())
        for (i in 1..size) {
            val a = MapWrapper(i, mutableMapOf())
            for (j in 1..size) {
                a.secondMap["$j"] = TestClass("$j", j)
            }
            mapInMapWrapper.map["$i"] = a
        }
        agentStore.store(mapInMapWrapper)
        println("Map in map stored")
        return mapInMapWrapper
    }

}
