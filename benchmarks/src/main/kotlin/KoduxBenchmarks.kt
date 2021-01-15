package kodux.benchmarks

import com.epam.kodux.*
import com.epam.kodux.encoder.*
import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.*
import java.io.*
import java.util.*
import java.util.concurrent.*

@KoduxDocument
data class Xs(@Id val st: String, val w: Int)

@Suppress("unused")
@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.NANOSECONDS)
class KoduxBenchmarks {

    private val storageDir = File("build/tmp/test/storages/${this::class.simpleName}-${UUID.randomUUID()}")

    private val storeClient = StoreClient(PersistentEntityStores.newInstance(storageDir))

    @TearDown
    fun after() {
        storeClient.close()
        storageDir.deleteRecursively()
    }

    @Benchmark
    fun put(bh: Blackhole) = runBlocking<Unit> {
        val any = Xs("x", 1)
        mstb().apply {
            this.findEntity(any)?.apply {
                deleteEntityRecursively(this)
            }
            val obj = this.newEntity(any::class.simpleName.toString())
            XodusEncoder(this, any::class.java.classLoader, obj).encodeSerializableValue(Xs.serializer(), any)
        }
    }

}
