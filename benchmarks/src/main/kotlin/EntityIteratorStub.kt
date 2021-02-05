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