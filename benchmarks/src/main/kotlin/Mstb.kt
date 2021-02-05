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

import jetbrains.exodus.entitystore.*
import java.util.Comparator

class mstb: StoreTransaction {
    override fun getAll(entityType: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun saveEntity(entity: Entity) {
        TODO("Not yet implemented")
    }

    override fun isReadonly(): Boolean {
        TODO("Not yet implemented")
    }

    override fun flush(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSnapshot(): StoreTransaction {
        TODO("Not yet implemented")
    }

    override fun sortLinks(entityType: String, sortedLinks: EntityIterable, isMultiple: Boolean, linkName: String, rightOrder: EntityIterable): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun sortLinks(entityType: String, sortedLinks: EntityIterable, isMultiple: Boolean, linkName: String, rightOrder: EntityIterable, oppositeEntityType: String, oppositeLinkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun commit(): Boolean {
        TODO("Not yet implemented")
    }

    override fun abort() {
        TODO("Not yet implemented")
    }

    override fun revert() {
        TODO("Not yet implemented")
    }

    override fun isIdempotent(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getEntityTypes(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun findWithProp(entityType: String, propertyName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun getSingletonIterable(entity: Entity): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun getStore(): EntityStore {
        TODO("Not yet implemented")
    }

    override fun findIds(entityType: String, minValue: Long, maxValue: Long): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun toEntityId(representation: String): EntityId {
        TODO("Not yet implemented")
    }

    override fun getSequence(sequenceName: String): Sequence {
        TODO("Not yet implemented")
    }

    override fun findWithBlob(entityType: String, blobName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun find(entityType: String, propertyName: String, value: Comparable<Nothing>): EntityIterable =EntityIterableStub()

    override fun find(entityType: String, propertyName: String, minValue: Comparable<Nothing>, maxValue: Comparable<Nothing>): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun sort(entityType: String, propertyName: String, ascending: Boolean): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun sort(entityType: String, propertyName: String, rightOrder: EntityIterable, ascending: Boolean): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun setQueryCancellingPolicy(policy: QueryCancellingPolicy?) {
        TODO("Not yet implemented")
    }

    override fun newEntity(entityType: String): Entity {
       return EntityStub()
    }

    override fun getEntity(id: EntityId): Entity {
        TODO("Not yet implemented")
    }

    override fun findStartingWith(entityType: String, propertyName: String, value: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun findWithLinks(entityType: String, linkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun findWithLinks(entityType: String, linkName: String, oppositeEntityType: String, oppositeLinkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun findLinks(entityType: String, entity: Entity, linkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun findLinks(entityType: String, entities: EntityIterable, linkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun mergeSorted(sorted: MutableList<EntityIterable>, comparator: Comparator<Entity>): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun isFinished(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getQueryCancellingPolicy(): QueryCancellingPolicy? {
        TODO("Not yet implemented")
    }

}