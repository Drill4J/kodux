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
@file:Suppress("BlockingMethodInNonBlockingContext")

package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.junit.Test
import java.io.*
import java.util.*
import kotlin.test.*

class XodusTest {

    private val last = Last(2.toByte())
    private val blink = SubObject("subStr", 12, last)
    private val complexObject = ComplexObject("str", 'x', blink, EnumExample.SECOND, null)
    private val simpleObject = SimpleObject("id", "subStr", 12, last)

    private val storageDir = File("build/tmp/test/storages/${this::class.simpleName}-${UUID.randomUUID()}")
    private val agentStore = StoreClient(PersistentEntityStores.newInstance(storageDir))
    private val pathToEntity = "\\${MapInMapWrapper::class.simpleName}\\map\\map\\value"

    @AfterTest
    fun after() {
        agentStore.close()
        storageDir.deleteRecursively()
    }

    @Test
    fun `should store and retrieve an object with composite id`() = runBlocking {
        val id = CompositeId("one", 1)
        val data = CompositeData(id, "data")
        agentStore.store(data)
        val all = agentStore.getAll<CompositeData>()
        assertEquals(1, all.count())
        val foundById = agentStore.findById<CompositeData>(CompositeId("one", 1))
        assertEquals(data, foundById)
        val foundByExprWithId = agentStore.findBy<CompositeData> { CompositeData::id eq id }
        assertEquals(data, foundByExprWithId.first())
        val foundByExprWithData = agentStore.findBy<CompositeData> { CompositeData::data eq "data" }
        assertEquals(data, foundByExprWithData.first())
    }

    @Test
    fun `should correctly update an object with composite id`() = runBlocking {
        val id = CompositeId("one", 1)
        updateObject(CompositeData(id, "data"), id, agentStore)
        updateObject(CompositeData(id, "data2"), id, agentStore)
    }

    @Test
    fun `should correctly create,update,delete an object with id and extra one annotation`() = runBlocking {
        val id = CompositeId("one", 1)
        updateObject(ObjectWithTwoAnnotation(id, 100), id, agentStore)
        updateObject(ObjectWithTwoAnnotation(id, 2000), id, agentStore)
        agentStore.deleteById<ObjectWithTwoAnnotation>(id)
        assertEquals(0, agentStore.getAll<ObjectWithTwoAnnotation>().count())
    }

    @Test
    fun `should store and retrieve a simple object`() = runBlocking {
        agentStore.store(simpleObject)
        val all = agentStore.getAll<SimpleObject>()
        val simpleObject1 = all.first()
        assertTrue(all.isNotEmpty())
        assertEquals(simpleObject1.id, "id")
        assertEquals(simpleObject1.int, 12)
        assertEquals(simpleObject1.string, "subStr")
        assertEquals(simpleObject1.last, last)
    }

    @Test
    fun `should store and retrieve an object with all-default payload`() = runBlocking {
        val withDefaults = ObjectWithDefaults("some-id")
        agentStore.store(withDefaults)
        val all = agentStore.getAll<ObjectWithDefaults>()
        assertEquals(listOf(withDefaults), all)
    }

    @Test
    fun `should store and retrieve a complex object`() = runBlocking {
        agentStore.store(complexObject)
        val all = agentStore.getAll<ComplexObject>()
        val cm = all.first()
        assertTrue(all.isNotEmpty())
        assertEquals(cm.id, "str")
        assertEquals(cm.ch, 'x')
        assertEquals(cm.blink, blink)
        assertEquals(cm.enumExample, EnumExample.SECOND)
        assertNull(cm.nullString)
    }

    @Test
    fun `should store and retrieve an object with complex nesting`() = runBlocking {
        val withDefaults = ComplexListNesting("some-id")
        agentStore.store(withDefaults)
        val all = agentStore.getAll<ComplexListNesting>()
        assertEquals(listOf(withDefaults), all)
    }

    @Test
    fun `should find object with complex expressions`() = runBlocking {
        agentStore.store(complexObject)
        agentStore.store(complexObject.copy(id = "123", ch = 'w'))
        val all = agentStore.findBy<ComplexObject> {
            (ComplexObject::enumExample eq EnumExample.SECOND) and (ComplexObject::id eq "123") and (ComplexObject::id eq "str")
        }
        assertTrue(all.isNotEmpty())
    }

    @Test
    fun `should remove entities of a complex object by ID recursively`() = runBlocking {
        agentStore.store(complexObject)
        agentStore.deleteById<ComplexObject>("str")
        assertTrue(agentStore.getAll<ComplexObject>().isEmpty())
        assertTrue(agentStore.getAll<SubObject>().isEmpty())
        assertTrue(agentStore.getAll<Last>().isEmpty())
    }

    @Test
    fun `should remove entities of a complex object by Prop recursively`() = runBlocking {
        agentStore.store(complexObject)
        agentStore.deleteBy<ComplexObject> { ComplexObject::enumExample eq EnumExample.SECOND }
        assertTrue(agentStore.getAll<ComplexObject>().isEmpty())
        assertTrue(agentStore.getAll<SubObject>().isEmpty())
        assertTrue(agentStore.getAll<Last>().isEmpty())
    }


    @Test
    fun `should store linked objects`() = runBlocking {
        agentStore.store(complexObject)
        assertTrue(agentStore.getAll<Last>().isNotEmpty())
    }

    @Test
    fun `should update object`() = runBlocking {
        agentStore.store(complexObject)
        agentStore.store(complexObject.copy(ch = 'y'))
        assertEquals(agentStore.getAll<ComplexObject>().first().ch, 'y')
    }

    @Test
    fun `should store several objects`() = runBlocking {
        agentStore.store(complexObject.copy(id = "1"))
        agentStore.store(complexObject.copy(id = "2"))
        val all = agentStore.getAll<ComplexObject>()
        assertEquals(2, all.size)
    }

    @Test
    fun `should store and retrieve object with primitive collection`() = runBlocking {
        agentStore.store(ObjectWithPrimitiveElementsCollection(1, listOf("st1", "st2")))
        val all = agentStore.getAll<ObjectWithPrimitiveElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference collection`() = runBlocking {
        agentStore.store(ObjectWithReferenceElementsCollection(1, setOf(TempObject("st", 2))))
        val all = agentStore.getAll<ObjectWithReferenceElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with primitive map`() = runBlocking {
        agentStore.store(ObjectWithPrimitiveElementsMap(1, mapOf("x" to 2)))
        val all = agentStore.getAll<ObjectWithPrimitiveElementsMap>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with mixed map - Simple key and complex value`() = runBlocking {
        agentStore.store(ObjectWithReferenceElementsMapMixed(1, mapOf("x" to TempObject("stsa", 3))))
        val all = agentStore.getAll<ObjectWithReferenceElementsMapMixed>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference map`() = runBlocking {
        agentStore.store(ObjectWithReferenceElementsMap(3, mapOf(TempObject("st", 2) to TempObject("stsa", 3))))
        val all = agentStore.getAll<ObjectWithReferenceElementsMap>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }


    @Serializable
    data class ObjectWithList(@Id val id: String, val primitiveList: List<Boolean>)

    @Test
    fun `should restore list related object with right order`() = runBlocking {
        val primitiveList = listOf(true, false, false, true)
        agentStore.store(ObjectWithList("id", primitiveList))
        val actual = agentStore.findById<ObjectWithList>("id")
        assertNotNull(actual)
        actual.primitiveList.forEachIndexed { index, pred ->
            assertEquals(primitiveList[index], pred)
        }
    }

    @Test
    fun `should be transactional`() = runBlocking {
        try {
            @Suppress("IMPLICIT_NOTHING_AS_TYPE_PARAMETER")
            agentStore.executeInAsyncTransaction {
                store(complexObject)
                fail("test")
            }
        } catch (ignored: Throwable) {
        }
        assertTrue(agentStore.getAll<ComplexObject>().isEmpty())
    }

    @Test
    fun `should not store old entities when set field gets updated`() = runBlocking {
        val set = mutableSetOf(
            SetPayload("1", "name1")
        )
        val obj = ObjectWithSetField("myId", set)
        agentStore.store(obj)
        assertEquals(1, agentStore.findById<ObjectWithSetField>("myId")?.set?.count())
        assertEquals("name1", agentStore.findById<ObjectWithSetField>("myId")?.set?.firstOrNull()?.name)
        val storedObj = agentStore.findById<ObjectWithSetField>("myId")!!
        storedObj.set.removeAll { it.id == "1" }
        storedObj.set.add(SetPayload("1", "name2"))
        agentStore.store(storedObj)
        assertEquals(1, agentStore.findById<ObjectWithSetField>("myId")?.set?.count())
        assertEquals("name2", agentStore.findById<ObjectWithSetField>("myId")?.set?.firstOrNull()?.name)
        val payloads = agentStore.getAll<SetPayload>()
        assertEquals(1, payloads.count())
    }

    @Test
    fun `should preserve and restore objects with byte arrays among fields`() = runBlocking {
        val testArray = "test".toByteArray()
        val obj = ObjectWithByteArray("myArray", testArray)
        agentStore.store(obj)
        val retrieved = agentStore.findById<ObjectWithByteArray>("myArray")
        assertTrue(testArray.contentEquals(retrieved?.array!!))
    }

    @Test
    fun `should preserve and retrieve map fields with Enum keys`() = runBlocking {
        val obj = MapField("test", mapOf(EnumExample.FIRST to TempObject("a", 5)))
        agentStore.store(obj)
        val retrievedMap = agentStore.findById<MapField>("test")?.map.orEmpty()
        assertEquals(5, retrievedMap[EnumExample.FIRST]?.int)
    }

    @Test
    fun `should delete all entities for a specified class`() = runBlocking<Unit> {
        val obj1 = StoreMe("id1")
        val obj2 = StoreMe("id2")
        val obj3 = MapField("id3")
        agentStore.store(obj1)
        agentStore.store(obj2)
        agentStore.store(obj3)
        assertEquals(2, agentStore.getAll<StoreMe>().count())
        assertNotNull(agentStore.findById<StoreMe>("id1"))
        assertNotNull(agentStore.findById<StoreMe>("id2"))
        assertNotNull(agentStore.findById<MapField>("id3"))
        agentStore.deleteAll<StoreMe>()
        assertEquals(0, agentStore.getAll<StoreMe>().count())
        assertNull(agentStore.findById<StoreMe>("id1"))
        assertNull(agentStore.findById<StoreMe>("id2"))
        assertNotNull(agentStore.findById<MapField>("id3") != null)
    }
}

}
