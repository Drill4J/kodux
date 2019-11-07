@file:Suppress("BlockingMethodInNonBlockingContext")

package com.epam.kodux

import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.junit.*
import org.junit.Test
import org.junit.rules.*
import kotlin.test.*

class XodusTest {
    private val agentId = "myAgent"
    @get:Rule
    val projectDir = TemporaryFolder()
    private lateinit var agentStore: StoreClient


    private val last = Last(2.toByte())
    private val blink = SubObject("subStr", 12, last)
    private val complexObject = ComplexObject("str", 'x', blink, EN.C, null)

    @BeforeTest
    fun before() {
        agentStore = StoreManager(projectDir.newFolder().resolve("agent")).agentStore(agentId)
    }

    @AfterTest
    fun after() {
        agentStore.close()
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
        assertEquals(cm.en, EN.C)
        assertNull(cm.nullString)
    }

    @Test
    fun `should find object with complex expressions`() = runBlocking {
        agentStore.store(complexObject)
        agentStore.store(complexObject.copy(id = "123", ch = 'w'))

        val all = agentStore.findBy<ComplexObject> { (ComplexObject::en eq EN.C) and (ComplexObject::id eq "123") and (ComplexObject::id eq "str") }
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
        agentStore.deleteBy<ComplexObject> { ComplexObject::en eq EN.C }
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
        agentStore.store(ObjectWithPrimitiveElementsCollection(listOf("st1", "st2"), 1))
        val all = agentStore.getAll<ObjectWithPrimitiveElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference collection`() = runBlocking {
        agentStore.store(ObjectWithReferenceElementsCollection(setOf(TempObject("st", 2)), 1))
        val all = agentStore.getAll<ObjectWithReferenceElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with primitive map`() = runBlocking {
        agentStore.store(ObjectWithPrimitiveElementsMap(mapOf("x" to 2), 1))
        val all = agentStore.getAll<ObjectWithPrimitiveElementsMap>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with mixed map - Simple key and complex value`() = runBlocking {
        agentStore.store(ObjectWithReferenceElementsMapMixed(mapOf("x" to TempObject("stsa", 3)), 1))
        val all = agentStore.getAll<ObjectWithReferenceElementsMapMixed>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference map`() = runBlocking {
        agentStore.store(ObjectWithReferenceElementsMap(mapOf(TempObject("st", 2) to TempObject("stsa", 3)), 3))
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
            agentStore.executeInAsyncTransaction {
                store(complexObject)
                fail("test")
            }
        } catch (ignored: Throwable) { }
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
        val obj = MapField("test", mapOf(EN.B to TempObject("a", 5)))
        agentStore.store(obj)
        val retrievedMap = agentStore.findById<MapField>("test")?.map.orEmpty()
        assertEquals(5, retrievedMap[EN.B]?.int)
    }

}
