package com.epam.kodux

import jetbrains.exodus.entitystore.*
import kotlin.reflect.*

class Expression<Q : Any> {
    lateinit var exprCallback: StoreTransaction.(KClass<Q>) -> EntityIterable
    fun process(transaction: StoreTransaction, cklas: KClass<Q>): EntityIterable {
        return exprCallback(transaction, cklas)
    }

    infix fun <Q, R : Comparable<*>> KProperty1<Q, R>.startsWith(r: R) {
        exprCallback = { it -> findStartingWith(it.simpleName.toString(), this@startsWith.name, r.toString()) }
    }

    infix fun <Q, R : Comparable<*>> KProperty1<Q, R>.eq(r: R) {
        exprCallback = { it -> find(it.simpleName.toString(), this@eq.name, r) }
    }
}