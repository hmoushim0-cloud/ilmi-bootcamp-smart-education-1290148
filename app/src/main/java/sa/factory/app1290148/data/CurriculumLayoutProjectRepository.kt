package sa.factory.app1290148.data

import sa.factory.app1290148.model.CurriculumLayoutProject
import sa.factory.app1290148.model.LayoutSpecificationPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface CurriculumLayoutProjectRepository {
    fun getAvailableCatalog(): Flow<List<LayoutSpecificationPage>>
    fun getActiveRequest(): Flow<CurriculumLayoutProject?>
    suspend fun submitRequest(request: CurriculumLayoutProject): Result<String>
}

class InMemoryCurriculumLayoutProjectRepository : CurriculumLayoutProjectRepository {
    private val _catalog = MutableStateFlow<List<LayoutSpecificationPage>>(listOf(
        LayoutSpecificationPage("1", "حزمة الإخراج الأساسي لكتيب التأسيس", "استخراج الصفحات الجاهزة وترتيب التسلسل التعليمي مع ضبط هوامش القص لعدد يصل إلى عشرين صفحة", 5000L, "verified"),
        LayoutSpecificationPage("2", "باقة المنهج المتكامل مع الصفحات الملونة", "تنسيق كامل مع تصميم خمس صفحات تفاعلية ملونة للألعاب اللغوية وتجهيز الغلاف للمطبعة", 9500L, "star")
    ))
    private val _active = MutableStateFlow<CurriculumLayoutProject?>(null)

    override fun getAvailableCatalog(): Flow<List<LayoutSpecificationPage>> = _catalog.asStateFlow()
    override fun getActiveRequest(): Flow<CurriculumLayoutProject?> = _active.asStateFlow()

    override suspend fun submitRequest(request: CurriculumLayoutProject): Result<String> {
        _active.value = request
        return Result.success(request.requestId)
    }
}
