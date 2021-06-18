package kodux.benchmarks

import com.epam.kodux.*
import jetbrains.exodus.entitystore.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.*
import org.openjdk.jmh.annotations.*
import java.io.*
import java.util.*
import java.util.concurrent.*

@Serializable
internal class StoredArray(
    @Id val id: String,
    val data: ByteArray,
)

@Serializable
internal class StoreKryo(
    @Id val id: String,
    @StreamSerialization(SerializationType.KRYO, CompressType.ZSTD, [])
    val data: HugeClass,
)

@Serializable
internal data class HugeClass(
    val name: String = "HugeClass",
    val strings: List<String>,
    val internClass: List<InternClass>,
)

@Serializable
internal data class InternClass(
    val strings: List<String>,
    val int: List<Int>,
    val map: Map<String, Int>,
)

@OptIn(ExperimentalSerializationApi::class)
@Suppress("unused")
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 5, timeUnit = TimeUnit.MILLISECONDS)
class KoduxBenchmarks {

    private val storageDir = File("build/tmp/test/storages/${this::class.simpleName}-${UUID.randomUUID()}")
    private lateinit var agentStore: StoreClient

    private val hugeClass: HugeClass

    private val id = "id"

    init {
        val strings: MutableList<String> = mutableListOf()
        val internClass: MutableList<InternClass> = mutableListOf()

        for (i in 0..1000) {
            strings.add("$i fgdfgdgd d$i fgdgf gd$i fd fd$i gdf $i t4123q fsfkljvdk z $i")
            val strings1: MutableList<String> = mutableListOf()
            val ints: MutableList<Int> = mutableListOf()
            val map: MutableMap<String, Int> = mutableMapOf()
            for (z in 0..1000) {
                val element = "$i fgdfgdgd d$i fgdgf gd$i fd fd$i gdf $i t4123q fsfkljvdk z $i"
                strings1.add(element)
                ints.add(i)
                map[element] = i
            }
            internClass.add(InternClass(strings1, ints, map))
        }

        hugeClass = HugeClass(strings = strings, internClass = internClass)
    }

    @Setup
    fun before() {
        agentStore = StoreClient(PersistentEntityStores.newInstance(storageDir))
    }

    @TearDown
    fun after() {
        agentStore.close()
        storageDir.deleteRecursively()
    }


//    @Benchmark
//    @BenchmarkMode(Mode.Throughput)
//    fun kryoStore() = runBlocking {
//        agentStore.store(StoreKryo(id, hugeClass))
//        Unit
//    }
//
//    @Benchmark
//    @BenchmarkMode(Mode.Throughput)
//    fun protoStore() = runBlocking {
//        val bytes = ProtoBuf.encodeToByteArray(HugeClass.serializer(), hugeClass)
//        val compressed = Zstd.compress(bytes)
//        agentStore.store(StoredArray(id, compressed))
//        Unit
//    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    fun kryoStoreAndLoad() = runBlocking {
        agentStore.store(StoreKryo(id, hugeClass))
        val loaded = agentStore.findById<StoreKryo>(id)!!
        println(loaded.data.name)
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    fun protoStoreAndLoad() = runBlocking {
        val bytes = ProtoBuf.encodeToByteArray(HugeClass.serializer(), hugeClass)
        val compressed = Zstd.compress(bytes)
        agentStore.store(StoredArray(id, compressed))

        val stored = agentStore.findById<StoredArray>(id)!!
        val deCompressed = Zstd.decompress(stored.data)
        val loaded = ProtoBuf.decodeFromByteArray(HugeClass.serializer(), deCompressed)
        println(loaded.name)
    }


}
