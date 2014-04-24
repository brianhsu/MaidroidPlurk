package idv.brianhsu.maidroid.plurk.util

import android.support.v4.util.{LruCache => JLruCache}

class LRUCache[K, V](maxItemCount: Int) {
  private val cache = new JLruCache[K, V](maxItemCount)
  
  def += (item: (K, V)) = {
    cache.put(item._1, item._2)
    this
  }
  
  def get(key: K): Option[V] = Option(cache.get(key))
}

