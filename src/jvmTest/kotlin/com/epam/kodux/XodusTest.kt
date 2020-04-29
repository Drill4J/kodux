@file:Suppress("BlockingMethodInNonBlockingContext")

package com.epam.kodux

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.*
import kotlin.random.*
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.*

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

    @ExperimentalTime
    @Test
    fun `big data test`() {
        val names = listOf("asdf", "asdf", "sdf", "asf")
        Random.nextInt()
        val data: (Int, Int) -> List<SomeData> = { n1, n2 ->
            List(n1) {
                SomeData(
                    id = UUID.randomUUID().toString(),
                    type = "123",
                    data = List(Random.nextInt(10, n2)) {
                        (1..10).fold(StringBuilder()) { acc, i ->
                            acc.append(UUID.randomUUID().toString())
                        }.toString()
                    }
                )
            }
        }
        val bigData = BigData(
            id = UUID.randomUUID().toString(),
            map = mapOf(
                "one" to data(1000, 100),
                "two" to data(200, 20)
            )
        )
        runBlocking {
            println("Storing")
            val measureTime = measureTime { agentStore.store(bigData) }
            println("Stored in $measureTime")
            println("Loading")
            val tt = measureTime { agentStore.findById<BigData>(bigData.id) }
            println("Loade in $tt")

        }

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
        val data = CompositeData(id, "data")
        agentStore.store(data)
        val all = agentStore.getAll<CompositeData>()
        assertEquals(1, all.count())
        val foundById = agentStore.findById<CompositeData>(CompositeId("one", 1))
        assertEquals(data, foundById)
        val data2 = CompositeData(id, "data2")
        agentStore.store(data2)
        assertEquals(1, agentStore.getAll<CompositeData>().count())
        @Suppress("RemoveExplicitTypeArguments")
        assertEquals(data2, agentStore.findById<CompositeData>(id))
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
    fun `should find object with complex expressions`() = runBlocking<Unit> {
        agentStore.store(complexObject)
        agentStore.store(complexObject.copy(id = "123", ch = 'w'))

        val all = agentStore.findBy<ComplexObject> { (ComplexObject::en eq EN.C) and (ComplexObject::id eq "123") and (ComplexObject::id eq "str") }
        assertTrue(all.isNotEmpty())
    }

//    @Test
//    fun `xdfg`() = runBlocking<Unit> {
//        val serializer = ComplexObject.serializer()
//        val serializer1 = EN.serializer()
//        println()
//    }

    @Test
    fun `should remove entities of a complex object by ID recursively`() = runBlocking<Unit> {
        agentStore.store(complexObject)
        agentStore.deleteById<ComplexObject>("str")
        assertTrue(agentStore.getAll<ComplexObject>().isEmpty())
        assertTrue(agentStore.getAll<SubObject>().isEmpty())
        assertTrue(agentStore.getAll<Last>().isEmpty())
    }

    @Test
    fun `should remove entities of a complex object by Prop recursively`() = runBlocking<Unit> {
        agentStore.store(complexObject)
        agentStore.deleteBy<ComplexObject> { ComplexObject::en eq EN.C }
        assertTrue(agentStore.getAll<ComplexObject>().isEmpty())
        assertTrue(agentStore.getAll<SubObject>().isEmpty())
        assertTrue(agentStore.getAll<Last>().isEmpty())
    }


    @Test
    fun `should store linked objects`() = runBlocking<Unit> {
        agentStore.store(complexObject)
        assertTrue(agentStore.getAll<Last>().isNotEmpty())
    }

    @Test
    fun `should update object`() = runBlocking<Unit> {
        agentStore.store(complexObject)
        agentStore.store(complexObject.copy(ch = 'y'))
        assertEquals(agentStore.getAll<ComplexObject>().first().ch, 'y')
    }

    @Test
    fun `should store several objects`() = runBlocking<Unit> {
        agentStore.store(complexObject.copy(id = "1"))
        agentStore.store(complexObject.copy(id = "2"))
        val all = agentStore.getAll<ComplexObject>()
        assertEquals(2, all.size)
    }

    @Test
    fun `should store and retrieve object with primitive collection`() = runBlocking<Unit> {
        agentStore.store(ObjectWithPrimitiveElementsCollection(listOf("st1", "st2"), 1))
        val all = agentStore.getAll<ObjectWithPrimitiveElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference collection`() = runBlocking<Unit> {
        agentStore.store(ObjectWithReferenceElementsCollection(setOf(TempObject("st", 2)), 1))
        val all = agentStore.getAll<ObjectWithReferenceElementsCollection>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with primitive map`() = runBlocking<Unit> {
        agentStore.store(ObjectWithPrimitiveElementsMap(mapOf("x" to 2), 1))
        val all = agentStore.getAll<ObjectWithPrimitiveElementsMap>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with mixed map - Simple key and complex value`() = runBlocking<Unit> {
        agentStore.store(ObjectWithReferenceElementsMapMixed(mapOf("x" to TempObject("stsa", 3)), 1))
        val all = agentStore.getAll<ObjectWithReferenceElementsMapMixed>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }

    @Test
    fun `should store and retrieve object with reference map`() = runBlocking<Unit> {
        agentStore.store(ObjectWithReferenceElementsMap(mapOf(TempObject("st", 2) to TempObject("stsa", 3)), 3))
        val all = agentStore.getAll<ObjectWithReferenceElementsMap>()
        assertTrue(all.isNotEmpty())
        assertTrue(all.first().st.isNotEmpty())
    }


    @Serializable
    data class ObjectWithList(@Id val id: String, val primitiveList: List<Boolean>)

    @Test
    fun `should restore list related object with right order`() = runBlocking<Unit> {
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
    fun `should not store old entities when set field gets updated`() = runBlocking<Unit> {
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
    fun `should preserve and restore objects with byte arrays among fields`() = runBlocking<Unit> {
        val testArray = "test".toByteArray()
        val obj = ObjectWithByteArray("myArray", testArray)
        agentStore.store(obj)
        val retrieved = agentStore.findById<ObjectWithByteArray>("myArray")
        assertTrue(testArray.contentEquals(retrieved?.array!!))
    }

    @Test
    fun `should preserve and retrieve map fields with Enum keys`() = runBlocking<Unit> {
        val obj = MapField("test", mapOf(EN.B to TempObject("a", 5)))
        agentStore.store(obj)
        val retrievedMap = agentStore.findById<MapField>("test")?.map.orEmpty()
        assertEquals(5, retrievedMap[EN.B]?.int)
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
