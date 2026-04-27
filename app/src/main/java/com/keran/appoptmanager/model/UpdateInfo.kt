package com.keran.appoptmanager.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    @SerialName("version_code")
    val versionCode: Int,
    @SerialName("version_name")
    val versionName: String,
    @SerialName("release_time")
    val releaseTime: String,
    @SerialName("apk_size")
    val apkSize: String,
    val changes: List<String>,
    @SerialName("download_url")
    val downloadUrl: String
)
