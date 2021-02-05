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
package kodux.benchmarks

import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.EntityIterable
import jetbrains.exodus.entitystore.EntityIterator
import jetbrains.exodus.entitystore.StoreTransaction

class EntityIterableStub: EntityIterable {
    override fun contains(entity: Entity): Boolean {
        TODO("Not yet implemented")
    }

    override fun concat(right: EntityIterable): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun take(number: Int): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun getLast(): Entity? {
        TODO("Not yet implemented")
    }

    override fun count(): Long {
        TODO("Not yet implemented")
    }

    override fun isSortResult(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): EntityIterator {
      return EntityIteratorStub()
    }

    override fun size(): Long {
        TODO("Not yet implemented")
    }

    override fun getRoughCount(): Long {
        TODO("Not yet implemented")
    }

    override fun selectDistinct(linkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun reverse(): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun indexOf(entity: Entity): Int {
        TODO("Not yet implemented")
    }

    override fun getFirst(): Entity? {
        TODO("Not yet implemented")
    }

    override fun asSortResult(): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun skip(number: Int): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun minus(right: EntityIterable): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun intersect(right: EntityIterable): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun getTransaction(): StoreTransaction {
        TODO("Not yet implemented")
    }

    override fun distinct(): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun intersectSavingOrder(right: EntityIterable): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun union(right: EntityIterable): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun selectManyDistinct(linkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun getRoughSize(): Long {
        TODO("Not yet implemented")
    }
}