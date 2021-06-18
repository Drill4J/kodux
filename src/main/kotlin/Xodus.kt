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
@file:Suppress("CovariantEquals")

package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*

class StoreClient(private val store: PersistentEntityStoreImpl) : PersistentEntityStore by store {

    suspend fun <T> executeInAsyncTransaction(block: suspend KoduxTransaction.() -> T) = withContext(Dispatchers.IO) {
        var result: T
        var txn: StoreTransaction? = beginTransaction()
        try {
            do {
                result = block(txn!!)
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

    suspend inline fun <reified T : Any> store(any: T, classLoader1: ClassLoader? = null) =
        withContext(Dispatchers.IO) { executeInTransaction { txn -> txn.store(any, classLoader1) };any }

    suspend inline fun <reified T : Any> getAll(): Collection<T> =
        withContext(Dispatchers.IO) { computeInReadonlyTransaction { txn -> txn.getAll() } }

    suspend inline fun <reified T : Any> findById(id: Any, classLoader1: ClassLoader? = null): T? =
        withContext(Dispatchers.IO) { computeInReadonlyTransaction { txn -> txn.findById<T>(id, classLoader1) } }


    suspend inline fun <reified T : Any> findBy(
        classLoader1: ClassLoader? = null,
        noinline expression: Expression<T>.() -> Unit,
    ) = withContext(Dispatchers.IO) { computeInTransaction { txn -> txn.findBy(expression, classLoader1) } }

    suspend inline fun <reified T : Any> deleteAll(): Unit =
        withContext(Dispatchers.IO) { computeInTransaction { txn -> txn.deleteAll<T>() } }

    suspend inline fun <reified T : Any> deleteById(id: Any): Unit = withContext(Dispatchers.IO) {
        computeInTransaction { txn -> txn.deleteById<T>(id) }
    }

    suspend inline fun <reified T : Any> deleteBy(noinline expression: Expression<T>.() -> Unit): Unit =
        withContext(Dispatchers.IO) {
            computeInTransaction { txn ->
                txn.deleteBy(expression)
            }
        }

}

