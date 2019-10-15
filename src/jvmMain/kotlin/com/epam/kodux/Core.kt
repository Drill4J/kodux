package com.epam.kodux

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.StoreTransaction
import kotlinx.serialization.decode
import kotlinx.serialization.encode
import kotlinx.serialization.serializer


typealias KoduxTransaction = StoreTransaction

inline fun <reified T : Any> KoduxTransaction.store(any: T) {
    this.findEntity(any)?.apply {
        deleteEntityRecursively(this)
    }
    val obj = this.newEntity(any::class.simpleName.toString())
    XodusEncoder(this, obj).encode(T::class.serializer(), any)
}

inline fun <reified T : Any> KoduxTransaction.getAll(): List<T> {
    return this.getAll(T::class.simpleName.toString()).map {
        XodusDecoder(this, it).decode(T::class.serializer())
    }
}

inline fun <reified T : Any> KoduxTransaction.findById(id: Any): T? {
    val serializer = T::class.serializer()
    val ent = this.find(T::class.simpleName!!, idName(serializer.descriptor)!!, id as Comparable<*>).firstOrNull()
            ?: return null
    return XodusDecoder(this, ent).decode(serializer)
}

inline fun <reified T : Any> KoduxTransaction.findBy(noinline expression: Expression<T>.() -> Unit) =
        this.computeWithExpression(expression, Expression())

inline fun <reified T : Any> KoduxTransaction.deleteById(id: Any) {
    val serializer = T::class.serializer()
    val ent = this.find(T::class.simpleName!!, idName(serializer.descriptor)!!, id as Comparable<*>).firstOrNull()
            ?: return
    deleteEntityRecursively(ent)
}

inline fun <reified T : Any> KoduxTransaction.deleteBy(expression: Expression<T>.() -> Unit) {
    val expr = Expression<T>()
    expression(expr)
    val entityIterable = expr.process(this, T::class)
    entityIterable.forEach { deleteEntityRecursively(it) }
}

fun deleteEntityRecursively(entity: Entity) {
    entity.linkNames.forEach { lName ->
        entity.getLinks(lName).forEach { subEnt -> deleteEntityRecursively(subEnt) }
        entity.deleteLinks(lName)
    }
    entity.delete()
}


inline fun <reified T : Any> KoduxTransaction.findEntity(any: T): Entity? {
    val (idName, value) = idPair(any)
    checkNotNull(idName) { "you should provide @Id for main entity" }
    checkNotNull(value)
    return this.find(T::class.simpleName.toString(), idName, value as Comparable<*>).firstOrNull()
}

inline fun <reified T : Any> KoduxTransaction.computeWithExpression(
        noinline expression: Expression<T>.() -> Unit, expr: Expression<T>
): List<T> = run {
    expression(expr)
    val entityIterable = expr.process(this, T::class)
    entityIterable.map {
        val strategy = T::class.serializer()
        XodusDecoder(this, it).decode(strategy)
    }
}
