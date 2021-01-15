package com.epam.kodux

import com.epam.kodux.decoder.*
import com.epam.kodux.encoder.*
import jetbrains.exodus.entitystore.*
import kotlinx.serialization.*


typealias KoduxTransaction = StoreTransaction

inline fun <reified T : Any> KoduxTransaction.store(any: T) {
    this.findEntity(any)?.apply {
        deleteEntityRecursively(this)
    }
    val obj = this.newEntity(any::class.simpleName.toString())
    val classLoader = T::class.java.classLoader
    XodusEncoder(this, classLoader, obj).encodeSerializableValue(T::class.serializer(), any)
}

inline fun <reified T : Any> KoduxTransaction.getAll(): List<T> {
    val serializer = T::class.serializer()
    val idName = idName(serializer.descriptor)
    val classLoader = T::class.java.classLoader
    return this.getAll(T::class.simpleName!!).map {
        XodusDecoder(this, classLoader, it, idName).decodeSerializableValue(serializer)
    }
}

inline fun <reified T : Any> KoduxTransaction.findById(id: Any): T? {
    val serializer = T::class.serializer()
    val idName = idName(serializer.descriptor)!!
    val classLoader = T::class.java.classLoader
    return findById<T>(idName, id)?.let {
        XodusDecoder(this, classLoader, it, idName).decodeSerializableValue(serializer)
    }
}

inline fun <reified T : Any> KoduxTransaction.findBy(
    noinline expression: Expression<T>.() -> Unit
): List<T> = run {
    val serializer = T::class.serializer()
    val idName = idName(serializer.descriptor)!!
    computeWithExpression(expression, Expression(idName))
}

inline fun <reified T : Any> KoduxTransaction.deleteAll() {
    this.getAll(T::class.simpleName.toString()).forEach { deleteEntityRecursively(it) }
}

inline fun <reified T : Any> KoduxTransaction.deleteById(id: Any) {
    val serializer = T::class.serializer()
    val idName = idName(serializer.descriptor)!!
    val encodedId = id.encodeId()
    find(T::class.simpleName!!, idName, encodedId).first?.let {
        deleteEntityRecursively(it)
    }
}

inline fun <reified T : Any> KoduxTransaction.deleteBy(expression: Expression<T>.() -> Unit) {
    val serializer = T::class.serializer()
    val idName = idName(serializer.descriptor)!!
    val expr = Expression<T>(idName)
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
    val (idName, id) = idPair(any)
    checkNotNull(idName) { "you should provide @Id for main entity" }
    checkNotNull(id)
    return findById<T>(idName, id)
}

inline fun <reified T : Any> KoduxTransaction.findById(
    idName: String,
    id: Any
): Entity? = find(T::class.simpleName!!, idName, id.encodeId()).first

inline fun <reified T : Any> KoduxTransaction.computeWithExpression(
    noinline expression: Expression<T>.() -> Unit, expr: Expression<T>
): List<T> = run {
    expression(expr)
    val entityIterable = expr.process(this, T::class)
    val serializer = T::class.serializer()
    val idName = idName(serializer.descriptor)
    val classLoader = T::class.java.classLoader
    entityIterable.map { XodusDecoder(this, classLoader, it, idName).decodeSerializableValue(serializer) }
}
