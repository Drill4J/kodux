package com.epam.kodux

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.StoreTransaction
import kotlinx.serialization.decode
import kotlinx.serialization.encode
import kotlinx.serialization.serializer
import org.slf4j.*

val coreLogger: Logger = LoggerFactory.getLogger("Core")

typealias KoduxTransaction = StoreTransaction

inline fun <reified T : Any> KoduxTransaction.store(any: T) {
    val className = T::class.simpleName.toString()
    coreLogger.info("Storing entity of class $className; contents: $any")
    this.findEntity(any)?.apply {
        deleteEntityRecursively(this)
    }
    val obj = this.newEntity(className)
    XodusEncoder(this, obj).encode(T::class.serializer(), any)
    coreLogger.info("Entity has been stored; contents: $any")
}

inline fun <reified T : Any> KoduxTransaction.getAll(): List<T> {
    val className = T::class.simpleName.toString()
    coreLogger.info("Getting all the entities for class $className")
    return this.getAll(className).map {
        XodusDecoder(this, it).decode(T::class.serializer())
    }
}

inline fun <reified T : Any> KoduxTransaction.findById(id: Any): T? {
    val className = T::class.simpleName.toString()
    coreLogger.info("Looking for entity by ID: $id (class: $className)")
    val serializer = T::class.serializer()
    val ent = this.find(className, idName(serializer.descriptor)!!, id as Comparable<*>).firstOrNull()
            ?: return null
    return XodusDecoder(this, ent).decode(serializer)
}

inline fun <reified T : Any> KoduxTransaction.findBy(noinline expression: Expression<T>.() -> Unit) = run {
    coreLogger.info("Searching for entities by expression: $expression")
    this.computeWithExpression(expression, Expression())
}

inline fun <reified T : Any> KoduxTransaction.deleteById(id: Any) {
    val className = T::class.simpleName.toString()
    coreLogger.info("Deleting entity by ID: $id (class: $className)")
    val serializer = T::class.serializer()
    val ent = this.find(className, idName(serializer.descriptor)!!, id as Comparable<*>).firstOrNull()
            ?: return
    deleteEntityRecursively(ent)
}

inline fun <reified T : Any> KoduxTransaction.deleteBy(expression: Expression<T>.() -> Unit) {
    val expr = Expression<T>()
    coreLogger.info("Deleting by expression: $expr")
    expression(expr)
    val entityIterable = expr.process(this, T::class)
    entityIterable.forEach { deleteEntityRecursively(it) }
}

fun deleteEntityRecursively(entity: Entity) {
    entity.linkNames.forEach { lName ->
        entity.getLinks(lName).forEach { subEnt -> deleteEntityRecursively(subEnt) }
        coreLogger.info("Deleting links from entity $entity to it's sub-entities")
        entity.deleteLinks(lName)
    }
    val entityId = entity.id
    coreLogger.info("Deleting entity with ID $entityId of type ${entity.type}")
    entity.delete()
    coreLogger.info("Entity with ID $entityId has been deleted")
}


inline fun <reified T : Any> KoduxTransaction.findEntity(any: T): Entity? {
    val className = T::class.simpleName.toString()
    coreLogger.info("Looking for entity; class: $className, contents: $any")
    val (idName, value) = idPair(any)
    checkNotNull(idName) { "you should provide @Id for main entity" }
    checkNotNull(value)
    coreLogger.info("Null-check for entity $any complete")
    return this.find(className, idName, value as Comparable<*>).firstOrNull()
}

inline fun <reified T : Any> KoduxTransaction.computeWithExpression(
        noinline expression: Expression<T>.() -> Unit, expr: Expression<T>
): List<T> = run {
    coreLogger.info("Computing expression: $expr")
    expression(expr)
    val entityIterable = expr.process(this, T::class)
    entityIterable.map {
        val strategy = T::class.serializer()
        XodusDecoder(this, it).decode(strategy)
    }
}
