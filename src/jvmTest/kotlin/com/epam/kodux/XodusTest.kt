@file:Suppress("BlockingMethodInNonBlockingContext")

package com.epam.kodux

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

enum class EN {
    B, C
}

@Serializable
data class ComplexObject(
        @Id val id: String,
        val ch: Char?,
        val blink: SubObject?,
        val en: EN = EN.B,
        val nullString: String?
)

@Serializable
data class SubObject(val string: String, val int: Int, val lst: Last)


@Serializable
data class Last(val string: Byte)


@Serializable
data class TempObject(val st: String, val int: Int)

@Serializable
data class ObjectWithPrimitiveElementsCollection(val st: List<String>, @Id val id: Int)

@Serializable
data class ObjectWithReferenceElementsCollection(val st: Set<TempObject>, @Id val id: Int)

@Serializable
data class ObjectWithPrimitiveElementsMap(val st: Map<String, Int>, @Id val id: Int)

@Serializable
data class ObjectWithReferenceElementsMap(val st: Map<TempObject, TempObject>, @Id val id: Int)

@Serializable
data class ObjectWithReferenceElementsMapMixed(val st: Map<String, TempObject>, @Id val id: Int)

@Serializable
data class FinishedSession(val st: String)


@Serializable
data class FinishedScope(
        @Id
        val id: String,
        val buildVersion: String,
        val name: String,
        val probes: Map<String, List<FinishedSession>>,
        var enabled: Boolean = true
) : Sequence<FinishedSession> {
    override fun iterator() = probes.values.flatten().iterator()
    override fun toString() = "fin-scope($id, $name)"
}


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
        agentStore = StoreManger(projectDir.newFolder().resolve("agent")).agentStore(agentId)
    }

    @AfterTest
    fun after() {
        agentStore.close()
    }

    @Test
    fun `should store and retrieve a complex object`() = runBlocking {
        agentStore.store(FinishedScope("", "", "", mutableMapOf("x" to mutableListOf(FinishedSession("xa"))), true))
        val all = agentStore.getAll<FinishedScope>()
        val actual = all.isNotEmpty()
        assertTrue(actual)
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
        agentStore.update(complexObject.copy(ch = 'y'))
        assertEquals(agentStore.getAll<ComplexObject>().first().ch, 'y')
    }

    @Test
    fun `should store several objects`() = runBlocking {
        agentStore.store(complexObject.copy(id = "1"))
        agentStore.store(complexObject.copy(id = "2"))
        val all = agentStore.getAll<ComplexObject>()
        assertEquals(2, all.size)
    }

    @Ignore
    @Test(expected = Exception::class)
    fun `should thrown an error if object is already exists`() = runBlocking {
        agentStore.store(complexObject.copy(id = "1"))
        agentStore.store(complexObject.copy(id = "1"))
        Unit
    }

    @Test(expected = Exception::class)
    fun `should throw an error if update a nonexistent object`() = runBlocking {
        agentStore.update(complexObject.copy(id = "1"))
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


}