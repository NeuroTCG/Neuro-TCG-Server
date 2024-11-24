package objects.shared

import kotlinx.serialization.Serializable
import kotlin.reflect.*

@Serializable
data class TrapCardStats(
    val activation: Activations,
    val effect: Effects,
)
