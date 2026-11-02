package sa.factory.app1290148.model

import kotlinx.serialization.Serializable

@Serializable
data class LayoutSpecificationPage(
    val id: String,
    val title: String,
    val description: String,
    val priceMinorUnits: Long,
    val iconVector: String = "verified"
)

@Serializable
data class CurriculumLayoutProject(
    val requestId: String,
    val items: List<LayoutSpecificationPage>,
    val customerPhone: String,
    val totalAmountMinorUnits: Long,
    val status: CurriculumLayoutProjectStatus = CurriculumLayoutProjectStatus.PENDING,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class CurriculumLayoutProjectStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
