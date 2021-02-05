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
import kotlin.reflect.*


class Expression<Q : Any>(private val idName: String) {
    var exprCallback: (StoreTransaction.(KClass<Q>) -> EntityIterable)? = null

    val subExpr: MutableSet<EntityIterable.() -> List<Entity>> = mutableSetOf()

    fun process(transaction: StoreTransaction, cklas: KClass<Q>): List<Entity> {
        if (exprCallback == null) return emptyList<Entity>()

        val mainSelection: EntityIterable = exprCallback!!(transaction, cklas)
        if (subExpr.isEmpty()) return mainSelection.toList()

        return subExpr.flatMap {
            it(mainSelection)
        }
    }

    infix fun <Q, R : Comparable<*>> KProperty1<Q, R>.startsWith(r: String) {
        exprCallback = {
            findStartingWith(it.simpleName.toString(), this@startsWith.name, r)
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    infix fun <R : Comparable<*>> KProperty1<Q, R>.eq(r: R): Expression<Q> {
        val toString = when {
            name == idName -> r.encodeId()
            r is Enum<*> -> r.ordinal
            else -> r.toString()
        } as Comparable<*>
        if (exprCallback == null)
            exprCallback = { it -> find(it.simpleName.toString(), this@eq.name, toString) }
        else {
            subExpr.add {
                filter { it.getProperty(this@eq.name) == toString }
            }
        }
        return this@Expression
    }


    infix fun Expression<Q>.and(@Suppress("UNUSED_PARAMETER") expression: Expression<Q>): Expression<Q> {
        return this@Expression
    }


}
