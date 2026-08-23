package com.gamestack.core.data.remote.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

internal val ApicalypsePlainText = "text/plain".toMediaType()

internal fun String.toApicalypseRequestBody(): RequestBody = toRequestBody(ApicalypsePlainText)