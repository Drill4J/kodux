@file:Suppress("CovariantEquals")

package com.epam.xodus

import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import java.io.*


class StoreClient(store: PersistentEntityStoreImpl) : PersistentEntityStore by store {
    suspend inline fun <reified T : Any> store(any: T) = withContext(Dispatchers.IO) {
        executeInTransaction { txn ->
            check(txn.findEntity(any) == null) { "Entity - '$any' already exists" }
            val obj = txn.newEntity(any::class.simpleName.toString())
            XodusEncoder(txn, obj).encode(T::class.serializer(), any)
        }
    }

    suspend inline fun <reified T : Any> update(any: T) = withContext(Dispatchers.IO) {
        executeInTransaction { txn: StoreTransaction ->
            val obj = txn.findEntity(any)
            checkNotNull(obj) { "Can't find the entity for - '$any'" }
            XodusEncoder(txn, obj).encode(T::class.serializer(), any)
        }
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
            XodusDecoder(
                    txn,
                    txn.find(T::class.simpleName!!, idName(serializer.descriptor)!!, id as Comparable<*>).first()
            ).decode(serializer)
        }
    }

    inline fun <reified T : Any> StoreTransaction.findEntity(any: T): Entity? {
        val (idName, value) = idPair(any)
        checkNotNull(idName)
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

    fun agentStore(agentId: String): StoreClient {
        val newInstance = PersistentEntityStores.newInstance(baseLocation.resolve(agentId))
        return StoreClient(newInstance)
    }
}

