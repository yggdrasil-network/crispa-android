package io.github.chronosx88.yggdrasil.models.config

import com.google.gson.annotations.SerializedName

data class SwitchOptions (
	@SerializedName("maxTotalQueueSize") val maxTotalQueueSize : Int // Maximum size of all switch queues combined (in bytes).
)