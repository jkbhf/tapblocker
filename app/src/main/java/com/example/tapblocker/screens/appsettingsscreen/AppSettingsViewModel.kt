import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tapblocker.data.AppDatabase
import com.example.tapblocker.data.AppSettingEntity
import com.example.tapblocker.data.RegionEntity
import com.example.tapblocker.data.RegionOrientation
import com.example.tapblocker.repository.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appName: String = "",
    val regions: List<RegionEntity> = emptyList()
)

class SettingsViewModel(
    private val repo: AppSettingsRepository,
    private val appId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            repo.observeSettings().collect { list ->
                list.find { it.app.id == appId }?.let { awr ->
                    _uiState.value = SettingsUiState(
                        appName = awr.app.name,
                        regions = awr.regions
                    )
                }
            }
        }
    }

    fun updateRegion(region: RegionEntity) = viewModelScope.launch {
        val current = _uiState.value.regions
        val updatedList = current.map { if (it.regionId == region.regionId) region else it }
        val appEntity = AppSettingEntity(id = appId, name = _uiState.value.appName, activated = true)
        repo.saveSetting(appEntity, updatedList)
    }

    fun addRegion() = viewModelScope.launch {
        val newRegion = RegionEntity(
            appId = appId,
            orientation = RegionOrientation.TOPLEFT.name,
            xOffset = 0,
            yOffset = 0,
            xSize = 100,
            ySize = 100
        )
        val updatedList = _uiState.value.regions + newRegion
        val appEntity = AppSettingEntity(id = appId, name = _uiState.value.appName, activated = true)
        repo.saveSetting(appEntity, updatedList)
    }

    fun deleteRegion(regionId: Long) = viewModelScope.launch {
        val updatedList = _uiState.value.regions.filterNot { it.regionId == regionId }
        val appEntity = AppSettingEntity(id = appId, name = _uiState.value.appName, activated = true)
        repo.saveSetting(appEntity, updatedList)
    }

    fun deleteApp() = viewModelScope.launch {
        repo.deleteApp(appId)
    }

    companion object {
        fun provideFactory(context: android.content.Context, appId: String)
                : androidx.lifecycle.ViewModelProvider.Factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val dao = AppDatabase.getInstance(context).appSettingsDao()
                    val repo = AppSettingsRepository(dao)
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(repo, appId) as T
                }
            }
    }
}
