package com.zeroproject.alice.app
data class AliceFeatureInfo(val feature: AliceFeature, val title: String)
object AliceFeatureCatalog {
    fun all(): List<AliceFeatureInfo> = AliceFeature.entries.map { AliceFeatureInfo(it, it.name.lowercase().replaceFirstChar { c -> c.uppercase() }) }
}
