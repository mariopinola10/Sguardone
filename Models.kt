package it.gerry.iptvlite.data

data class Channel(
    val id: Long,
    val name: String,
    val group: String,
    val url: String,
    val userAgent: String?,
    val referer: String?,
    val epgKey: String?,
)

data class Programme(
    val start: Long,
    val stop: Long,
    val title: String,
    val description: String?,
)

data class Group(val name: String, val count: Int)
