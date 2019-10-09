package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlin.reflect.*

class Expression<Q : Any> {
    lateinit var exprCallback: StoreTransaction.(KClass<Q>) -> EntityIterable
    fun process(transaction: StoreTransaction, cklas: KClass<Q>): EntityIterable {
        return exprCallback(transaction, cklas)
    }

    infix fun <Q, R : Comparable<*>> KProperty1<Q, R>.startsWith(r: String) {
        exprCallback = {
            findStartingWith(it.simpleName.toString(), this@startsWith.name, r)
        }
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    infix fun <Q, R : Comparable<*>> KProperty1<Q, R>.eq(r: R) {
        val toString = when (r) {
            is Enum<*> -> r.ordinal
            else -> r.toString()
        } as Comparable<*>
        exprCallback = { it -> find(it.simpleName.toString(), this@eq.name, toString) }
    }
}