package kodux.benchmarks

import com.epam.kodux.*
import com.epam.kodux.encoder.XodusEncoder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encode
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@KoduxDocument
data class Xs(@Id val st: String, val w: Int)

@Suppress("unused")
@State(Scope.Benchmark)
@Warmup(iterations = 10)
@Measurement(iterations = 3, time = 2, timeUnit = TimeUnit.NANOSECONDS)
class KoduxBenchmarks {

    var i = 0
    private lateinit var agentStore: StoreClient

    @Setup
    fun before() {
        agentStore = StoreManager(Files.createTempDirectory("bench").resolve("agent").toFile()).agentStore("my")
    }

    @TearDown
    fun after() {
        agentStore.close()
    }

    @Benchmark
    fun put(bh: Blackhole) = runBlocking<Unit> {
        val any = Xs("x", 1)
        mstb().apply {
            this.findEntity(any)?.apply {
                deleteEntityRecursively(this)
            }
            val obj = this.newEntity(any::class.simpleName.toString())
            XodusEncoder(this, obj).encodeSerializableValue(Xs.serializer(), any)
        }
    }

}