package kodux.benchmarks

import jetbrains.exodus.ByteIterable
import jetbrains.exodus.entitystore.Entity
import jetbrains.exodus.entitystore.EntityId
import jetbrains.exodus.entitystore.EntityIterable
import jetbrains.exodus.entitystore.EntityStore
import java.io.File
import java.io.InputStream

class EntityStub : Entity {
    override fun getRawProperty(propertyName: String): ByteIterable? {
        TODO("Not yet implemented")
    }

    override fun getLinks(linkName: String): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun getLinks(linkNames: MutableCollection<String>): EntityIterable {
        TODO("Not yet implemented")
    }

    override fun setBlob(blobName: String, blob: InputStream) {
        TODO("Not yet implemented")
    }

    override fun setBlob(blobName: String, file: File) {
        TODO("Not yet implemented")
    }

    override fun setBlobString(blobName: String, blobString: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getId(): EntityId {
        TODO("Not yet implemented")
    }

    override fun deleteLink(linkName: String, target: Entity): Boolean {
        TODO("Not yet implemented")
    }

    override fun setLink(linkName: String, target: Entity?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getProperty(propertyName: String): Comparable<Nothing> {
        TODO("Not yet implemented")
    }

    override fun getBlobNames(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun getLink(linkName: String): Entity? {
        TODO("Not yet implemented")
    }

    override fun deleteLinks(linkName: String) {
        TODO("Not yet implemented")
    }

    override fun getPropertyNames(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun getStore(): EntityStore {
        TODO("Not yet implemented")
    }

    override fun deleteBlob(blobName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun delete(): Boolean {
        TODO("Not yet implemented")
    }

    override fun addLink(linkName: String, target: Entity): Boolean {
        TODO("Not yet implemented")
    }

    override fun setProperty(propertyName: String, value: Comparable<Nothing>): Boolean {
        return true
    }

    override fun getLinkNames(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun compareTo(other: Entity?): Int {
        TODO("Not yet implemented")
    }

    override fun getType(): String {
        TODO("Not yet implemented")
    }

    override fun deleteProperty(propertyName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getBlobString(blobName: String): String? {
        TODO("Not yet implemented")
    }

    override fun toIdString(): String {
        TODO("Not yet implemented")
    }

    override fun getBlob(blobName: String): InputStream? {
        TODO("Not yet implemented")
    }

    override fun getBlobSize(blobName: String): Long {
        TODO("Not yet implemented")
    }
}