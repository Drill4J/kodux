package kodux.benchmarks

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.EntityId
import jetbrains.exodus.entitystore.EntityIterator

class EntityIteratorStub : EntityIterator {
    override fun next(): Entity {
        TODO("Not yet implemented")
    }

    override fun nextId(): EntityId? {
        TODO("Not yet implemented")
    }

    override fun skip(number: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun shouldBeDisposed(): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove() {
        TODO("Not yet implemented")
    }

    override fun hasNext(): Boolean {
        return false
    }

    override fun dispose(): Boolean {
        TODO("Not yet implemented")
    }
}