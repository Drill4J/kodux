@file:Suppress("CovariantEquals")

package com.epam.kodux

import jetbrains.exodus.entitystore.*
import jetbrains.exodus.env.Transaction
import kotlinx.coroutines.*
import kotlinx.serialization.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap


class StoreClient(val store: PersistentEntityStoreImpl, val unsafeMode: Boolean = false) : PersistentEntityStore by store {
    suspend inline fun <reified T : Any> store(any: T) = withContext(Dispatchers.IO) {
        executeInTransaction { txn ->
            if (txn.findEntity(any) != null) {
                txn.update(any)
            } else {
                val obj = txn.newEntity(any::class.simpleName.toString())
                XodusEncoder(txn, obj).encode(T::class.serializer(), any)
            }
        }
        any
    }

    suspend inline fun <reified T : Any> update(any: T) = withContext(Dispatchers.IO) {
        executeInTransaction { txn: StoreTransaction ->
            txn.update(any)
        }
    }

    inline fun <reified T : Any> StoreTransaction.update(any: T) {
        val obj = this.findEntity(any)
        checkNotNull(obj) { "Can't find the entity for - '$any'" }
        XodusEncoder(this, obj).encode(T::class.serializer(), any)
    }

    suspend inline fun <reified T : Any> getAll(): Collection<T> = withContext(Dispatchers.IO) {
        computeInReadonlyTransaction { txn ->
            txn.getAll(T::class.simpleName.toString()).map {
                XodusDecoder(txn, it).decode(T::class.serializer())
            }
        }
    }

    suspend inline fun <reified T : Any> findById(id: Any): T? = withContext(Dispatchers.IO) {
        computeInReadonlyTransaction { txn ->
            val serializer = T::class.serializer()
            val ent = txn.find(T::class.simpleName!!, idName(serializer.descriptor)!!, id as Comparable<*>).firstOrNull()
                    ?: return@computeInReadonlyTransaction null
            XodusDecoder(
                    txn,
                    ent
            ).decode(serializer)
        }
    }

    inline fun <reified T : Any> StoreTransaction.findEntity(any: T): Entity? {
        val (idName, value) = idPair(any)
        checkNotNull(idName) { "you should provide @Id for main entity" }
        checkNotNull(value)
        return this.find(T::class.simpleName.toString(), idName, value as Comparable<*>).firstOrNull()
    }

    inline fun <reified T : Any> findBy(crossinline expression: Expression<T>.() -> Unit) =
            runBlocking(Dispatchers.IO) { computeWithExpression(expression, Expression()) }


    inline fun <reified T : Any> computeWithExpression(
            crossinline expression: Expression<T>.() -> Unit, expr: Expression<T>
    ): List<T> = this.computeInTransaction { txn ->
        expression(expr)
        val entityIterable = expr.process(txn, T::class)
        entityIterable.map {
            val strategy = T::class.serializer()
            XodusDecoder(txn, it).decode(strategy)
        }
    }
}

class StoreManger(private val baseLocation: File = File("./").resolve("agent")) {
    val storages = ConcurrentHashMap<String, StoreClient>()
    fun agentStore(agentId: String): StoreClient {
        return storages.getOrPut(baseLocation.absolutePath, {
            val store = PersistentEntityStores.newInstance(baseLocation.resolve(agentId))

            StoreClient(store) })
    }
}

