package info.benjaminhill.gdrivedl

import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

/**
 * To cache a lot of small function calls to disk
 */
@ExperimentalTime
class SimpleCache<K : Serializable, V : Serializable> {
    private val cacheFile = File("simplecache.ser.gz")
    private val cache: MutableMap<K, V> = ConcurrentHashMap()
    private val mutationCount = AtomicLong()
    private val persistTime = AtomicLong()

    init {
        if (cacheFile.exists() && cacheFile.canRead()) {
            ObjectInputStream(GZIPInputStream(cacheFile.inputStream())).use { inputStream ->
                @Suppress("UNCHECKED_CAST")
                cache.putAll(inputStream.readObject() as Map<K, V>)
                println("SimpleCache startup loaded ${cache.size}")
            }
        }
    }

    private fun persistToDisk() {
        ObjectOutputStream(GZIPOutputStream(cacheFile.outputStream())).use {
            it.writeObject(cache)
            println("SimpleCache auto-persisted ${cache.size}")
        }
        mutationCount.set(0)
        persistTime.set(System.currentTimeMillis())
    }

    val size: Int
        get() = cache.size

    fun set(key: K, value: V) {
        this.cache[key] = value

        if (
            mutationCount.incrementAndGet() > 1000 ||
            System.currentTimeMillis() > persistTime.get() + 5.minutes.inMilliseconds
        ) {
            persistToDisk()
        }
    }

    fun get(key: Any) = this.cache[key]

    /** Load a cached object if available, calculate and cache if not. */
    fun getOrPut(key: K, exec: () -> V): V {
        if (!cache.containsKey(key)) {
            set(key, exec())
            println("  cache miss on '$key'")
        } else {
            println("  cache hit on '$key'")
        }
        return cache[key]!!
    }
}