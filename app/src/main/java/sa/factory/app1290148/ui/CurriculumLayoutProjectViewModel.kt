package sa.factory.app1290148.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sa.factory.app1290148.data.CurriculumLayoutProjectRepository
import sa.factory.app1290148.model.CurriculumLayoutProject
import sa.factory.app1290148.model.LayoutSpecificationPage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CurriculumLayoutProjectViewModel(
    private val repository: CurriculumLayoutProjectRepository
) : ViewModel() {
    val catalog: StateFlow<List<LayoutSpecificationPage>> = repository.getAvailableCatalog()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRequest: StateFlow<CurriculumLayoutProject?> = repository.getActiveRequest()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun submitNewOrder(request: CurriculumLayoutProject) {
        viewModelScope.launch {
            repository.submitRequest(request)
        }
    }
}
