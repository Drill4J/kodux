@file:Suppress("CovariantEquals")

package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.*


class StoreClient(val store: PersistentEntityStoreImpl, val unsafeMode: Boolean = false) : PersistentEntityStore by store {

    suspend fun <T> executeInAsyncTransaction(block: suspend KoduxTransaction.() -> T) = withContext(Dispatchers.IO) {
        var result: T
        var txn:StoreTransaction? = beginTransaction()
        try {
            do {
                result =  block(txn!!)
                if (txn !== currentTransaction) {
                    txn = null
                    break
                }
            } while (!txn!!.flush())
        } finally {
            txn?.abort()
        }
        result

    }


    suspend inline fun <reified T : Any> store(any: T) =
            withContext(Dispatchers.IO) { executeInTransaction { txn -> txn.store(any) };any }

    suspend inline fun <reified T : Any> getAll(): Collection<T> =
            withContext(Dispatchers.IO) { computeInReadonlyTransaction { txn -> txn.getAll<T>() } }

    suspend inline fun <reified T : Any> findById(id: Any): T? =
            withContext(Dispatchers.IO) { computeInReadonlyTransaction { txn -> txn.findById<T>(id) } }


    suspend inline fun <reified T : Any> findBy(noinline expression: Expression<T>.() -> Unit) =
        withContext(Dispatchers.IO) { computeInTransaction { txn -> txn.findBy(expression) } }

    suspend inline fun <reified T : Any> deleteAll(): Unit =
        withContext(Dispatchers.IO) { computeInTransaction { txn -> txn.deleteAll<T>() } }

    suspend inline fun <reified T : Any> deleteById(id: Any) = withContext(Dispatchers.IO) {
        computeInTransaction { txn -> txn.deleteById<T>(id) }
    }

    suspend inline fun <reified T : Any> deleteBy(noinline expression: Expression<T>.() -> Unit) =
            withContext(Dispatchers.IO) {
                computeInTransaction { txn ->
                    txn.deleteBy(expression)
                }
            }



}

class StoreManager(private val baseLocation: File = File("./").resolve("agent")) {
    val storages = ConcurrentHashMap<String, StoreClient>()
    fun agentStore(agentId: String): StoreClient {
        return storages.getOrPut(baseLocation.resolve(agentId).absolutePath, {
            val store = PersistentEntityStores.newInstance(baseLocation.resolve(agentId))

            StoreClient(store)
        })
    }
}

