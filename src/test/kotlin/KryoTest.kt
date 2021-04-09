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

import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.junit.Test
import java.io.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.*

class KryoTest {

    private val storageDir = File("build/tmp/test/storages/${this::class.simpleName}-${UUID.randomUUID()}")
    private val agentStore = StoreClient(PersistentEntityStores.newInstance(storageDir))
    private val pathToEntity = "\\${MapInMapWrapperKryo::class.simpleName}\\map\\map\\value"

    @AfterTest
    fun after() {
        agentStore.close()
        storageDir.deleteRecursively()
    }


    @Test
    fun `should correctly create,update,get and delete an object with id and StreamSerialization Kryo annotation`() =
        runBlocking {
            val id = CompositeId("one", 1)
            updateObject(StreamSerializationKryoTestObject(id, listOf("one", "two", "three")), id, agentStore)
            updateObject(StreamSerializationKryoTestObject(id, listOf("three", "two", "one")), id, agentStore)
            agentStore.deleteById<StreamSerializationTestObject>(id)
            assertEquals(0, agentStore.getAll<StreamSerializationTestObject>().count())
        }

    @Test
    fun `should correctly create,update,get and delete an object with id and StreamSerialization Kryo annotation1`() =
        runBlocking {
            val id = CompositeId("one", 1)
            updateObject(StreamSerializationKryoTestObject1(id, TestClass("1", 1)), id, agentStore)
            updateObject(StreamSerializationKryoTestObject1(id, TestClass("2", 2)), id, agentStore)
            agentStore.deleteById<StreamSerializationTestObject>(id)
            assertEquals(0, agentStore.getAll<StreamSerializationTestObject>().count())
        }


    @Test
    fun `should correctly create,update,get and delete an object with id and StreamSerialization Kryo2 annotation`() =
        runBlocking {
            val id = CompositeId("one", 1)
            updateObject(StreamSerializationKryoTestObjectTest(id, TestClass("1", 1)), id, agentStore)
            updateObject(StreamSerializationKryoTestObjectTest(id, TestClass("1", 3)), id, agentStore)
            agentStore.deleteById<StreamSerializationTestObject>(id)
            assertEquals(0, agentStore.getAll<StreamSerializationTestObject>().count())
        }

    @Test
    fun `should not delete files belonging to another entity`() = runBlocking {
        val firstId = CompositeId("one", 1)
        val mapInMapWrapper = mapInMapWrapperKryo(firstId)

        val secondId = CompositeId("two", 2)
        val mapInMapWrapper2 = mapInMapWrapperKryo(secondId)

        assertEquals(
            10,
            File("${agentStore.location}$pathToEntity").listFiles()?.size ?: 0
        )
        assertEquals(mapInMapWrapper, agentStore.findById<MapInMapWrapperKryo>(firstId))
        agentStore.deleteById<MapInMapWrapperKryo>(firstId)
        assertEquals(
            5,
            File("${agentStore.location}$pathToEntity").listFiles()?.size ?: 0
        )
        assertEquals(mapInMapWrapper2, agentStore.findById<MapInMapWrapperKryo>(secondId))
        agentStore.deleteById<MapInMapWrapperKryo>(secondId)
        assertEquals(
            0,
            File("${agentStore.location}$pathToEntity").listFiles()?.size ?: 1
        )
    }

    private suspend fun mapInMapWrapperKryo(id: CompositeId): MapInMapWrapperKryo {
        val mapInMapWrapper = MapInMapWrapperKryo(id, mutableMapOf())
        for (i in 1..5) {
            val a = MapWrapperKryo(i, mutableMapOf())
            for (j in 1..5) {
                a.secondMap["$j"] = TestClass("$j", j)
            }
            mapInMapWrapper.map["$i"] = a
        }
        agentStore.store(mapInMapWrapper)
        return mapInMapWrapper
    }

    @Test
    fun `should correctly create,update,get and delete Drill object`() =
        runBlocking {
            val id = "one"
            val a = StoredClassData(id, ClassData(
                "0.0.0",
                PackageTree(
                    1,
                    1,
                    1,
                    ArrayList<JavaPackageCoverage>().also { it2 ->
                        it2.add(
                            JavaPackageCoverage(
                                "1",
                                "1",
                                1,
                                1,
                                1,
                                1.0,
                                1,
                                1,
                                1,
                                ArrayList<JavaClassCoverage>().also { it1 ->
                                    it1.add(JavaClassCoverage(
                                        "1",
                                        "3",
                                        "fdfgd",
                                        1,
                                        1,
                                        1.0,
                                        1,
                                        1,
                                        ArrayList<JavaMethodCoverage>().also {
                                            it.add(
                                                JavaMethodCoverage(
                                                    "1",
                                                    "31",
                                                    "1",
                                                    "1",
                                                    0,
                                                    0.0,
                                                    0
                                                )
                                            )
                                        }
                                    ))
                                }
                            ))
                    }
                ),
                ArrayList<Method>().also {
                    it.add(Method("1", "2", "3", "gfd"))}
            ))
            updateObject(a, id, agentStore)
            updateObject(a, id, agentStore)
            agentStore.deleteById<StreamSerializationTestObject>(id)
            assertEquals(0, agentStore.getAll<StreamSerializationTestObject>().count())
        }


}


@Serializable
internal class StoredClassData(
    @Id val version: String,
    @StreamSerialization(SerializationType.KRYO, CompressType.ZSTD)
    val data: ClassData,
)

@Serializable
internal data class ClassData(
    @Id val buildVersion: String,
    val packageTree: PackageTree = PackageTree(),
    val methods: List<Method> = emptyList(),
    val probeIds: Map<String, Long> = emptyMap(),
)

@Serializable
data class PackageTree(
    val totalCount: Int = 0,
    val totalMethodCount: Int = 0,
    val totalClassCount: Int = 0,
    val packages: List<JavaPackageCoverage> = emptyList(),
)

@Serializable
data class JavaPackageCoverage(
    val id: String,
    val name: String,
    val totalClassesCount: Int = 0,
    val totalMethodsCount: Int = 0,
    val totalCount: Int = 0,
    val coverage: Double = 0.0,
    val coveredClassesCount: Int = 0,
    val coveredMethodsCount: Int = 0,
    val assocTestsCount: Int = 0,
    val classes: List<JavaClassCoverage> = emptyList(),
)

@Serializable
data class JavaClassCoverage(
    val id: String,
    val name: String,
    val path: String,
    val totalMethodsCount: Int = 0,
    val totalCount: Int = 0,
    val coverage: Double = 0.0,
    val coveredMethodsCount: Int = 0,
    val assocTestsCount: Int = 0,
    val methods: List<JavaMethodCoverage> = emptyList(),
    val probes: List<Int> = emptyList(),
)

@Serializable
data class JavaMethodCoverage(
    val id: String,
    val name: String,
    val desc: String,
    val decl: String,
    val count: Int,
    val coverage: Double = 0.0,
    val assocTestsCount: Int = 0,
)

@Serializable
internal data class Method(
    val ownerClass: String,
    val name: String,
    val desc: String,
    val hash: String,
)
