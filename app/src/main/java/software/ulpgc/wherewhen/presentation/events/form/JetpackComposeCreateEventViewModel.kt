package software.ulpgc.wherewhen.presentation.events.form

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import software.ulpgc.wherewhen.domain.exceptions.events.InvalidEventException
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.model.events.Location
import software.ulpgc.wherewhen.domain.model.events.Price
import software.ulpgc.wherewhen.domain.ports.location.LocationService
import software.ulpgc.wherewhen.domain.ports.storage.ImageUploadService
import software.ulpgc.wherewhen.domain.usecases.events.CreateUserEventUseCase
import software.ulpgc.wherewhen.domain.usecases.events.GetEventByIdUseCase
import software.ulpgc.wherewhen.domain.usecases.events.UpdateUserEventUseCase
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import software.ulpgc.wherewhen.domain.viewModels.CreateEventViewModel
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class JetpackComposeCreateEventViewModel(
    application: Application,
    private val createUserEventUseCase: CreateUserEventUseCase,
    private val updateUserEventUseCase: UpdateUserEventUseCase,
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val locationService: LocationService,
    private val imageUploadService: ImageUploadService
) : AndroidViewModel(application), CreateEventViewModel {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    var uiState by mutableStateOf<UiState>(UiState.Idle)
        private set

    var title by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    var selectedCategory by mutableStateOf(EventCategory.OTHER)
        private set

    var selectedDate by mutableStateOf(LocalDate.now().plusDays(1))
        private set

    var selectedTime by mutableStateOf(LocalTime.of(18, 0))
        private set

    var selectedEndDate by mutableStateOf<LocalDate?>(null)
        private set

    var selectedEndTime by mutableStateOf<LocalTime?>(null)
        private set

    var maxAttendees by mutableStateOf("")
        private set

    var locationAddress by mutableStateOf("")
        private set

    var currentLocation by mutableStateOf<Location?>(null)
        private set

    var useCurrentLocation by mutableStateOf(true)
        private set

    var imageUrl by mutableStateOf("")
        private set

    var selectedImageUri by mutableStateOf<Uri?>(null)
        private set

    var isUploadingImage by mutableStateOf(false)
        private set

    var isFreeEvent by mutableStateOf(true)
        private set

    var priceAmount by mutableStateOf("")
        private set

    var priceCurrency by mutableStateOf("EUR")
        private set

    var hasUnsavedChanges by mutableStateOf(false)
        private set

    var showExitDialog by mutableStateOf(false)
        private set

    private var isEditMode = false
    private var editingEventId: UUID? = null
    private var originalEvent: Event? = null

    init {
        loadCurrentLocation()
    }

    fun resetState() {
        uiState = UiState.Idle
        title = ""
        description = ""
        selectedCategory = EventCategory.OTHER
        selectedDate = LocalDate.now().plusDays(1)
        selectedTime = LocalTime.of(18, 0)
        selectedEndDate = null
        selectedEndTime = null
        maxAttendees = ""
        locationAddress = ""
        useCurrentLocation = true
        imageUrl = ""
        selectedImageUri = null
        isUploadingImage = false
        isFreeEvent = true
        priceAmount = ""
        priceCurrency = "EUR"
        hasUnsavedChanges = false
        showExitDialog = false
        isEditMode = false
        editingEventId = null
        originalEvent = null
        loadCurrentLocation()
    }

    fun loadEventToEdit(eventId: UUID) {
        viewModelScope.launch {
            uiState = UiState.Loading
            getEventByIdUseCase(eventId).fold(
                onSuccess = { event ->
                    isEditMode = true
                    editingEventId = eventId
                    originalEvent = event
                    title = event.title
                    description = event.description ?: ""
                    selectedCategory = event.category
                    selectedDate = event.dateTime.toLocalDate()
                    selectedTime = event.dateTime.toLocalTime()
                    selectedEndDate = event.endDateTime?.toLocalDate()
                    selectedEndTime = event.endDateTime?.toLocalTime()
                    maxAttendees = event.maxAttendees?.toString() ?: ""
                    locationAddress = event.location.address ?: ""
                    currentLocation = event.location
                    useCurrentLocation = false
                    imageUrl = event.imageUrl ?: ""
                    isFreeEvent = event.price?.isFree ?: true
                    priceAmount = if (event.price?.isFree == false) {
                        event.price?.min?.toString() ?: ""
                    } else ""
                    priceCurrency = event.price?.currency ?: "EUR"
                    hasUnsavedChanges = false
                    uiState = UiState.Idle
                },
                onFailure = { exception ->
                    uiState = UiState.Error("Failed to load event: ${exception.message}")
                }
            )
        }
    }

    override fun onTitleChange(value: String) {
        title = value
        checkForChanges()
    }

    override fun onDescriptionChange(value: String) {
        description = value
        checkForChanges()
    }

    override fun onCategoryChange(category: EventCategory) {
        selectedCategory = category
        checkForChanges()
    }

    override fun onDateChange(date: LocalDate) {
        selectedDate = date
        checkForChanges()
    }

    override fun onTimeChange(time: LocalTime) {
        selectedTime = time
        checkForChanges()
    }

    override fun onEndDateChange(date: LocalDate?) {
        selectedEndDate = date
        checkForChanges()
    }

    override fun onEndTimeChange(time: LocalTime?) {
        selectedEndTime = time
        checkForChanges()
    }

    override fun onMaxAttendeesChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) {
            maxAttendees = value
            checkForChanges()
        }
    }

    override fun onLocationAddressChange(value: String) {
        locationAddress = value
        checkForChanges()
    }

    override fun onUseCurrentLocationChange(value: Boolean) {
        useCurrentLocation = value
        if (value) {
            loadCurrentLocation()
        }
        checkForChanges()
    }

    fun onImageUrlChange(value: String) {
        imageUrl = value
        checkForChanges()
    }

    fun onImageSelected(uri: Uri?) {
        selectedImageUri = uri
        checkForChanges()
    }

    private fun compressImage(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val compressedFile = File.createTempFile(
                "compressed_",
                ".jpg",
                context.cacheDir
            )

            val outputStream = FileOutputStream(compressedFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.close()
            bitmap.recycle()

            compressedFile
        } catch (e: Exception) {
            Log.e("CreateEventViewModel", "Error compressing image", e)
            null
        }
    }

    fun onIsFreeEventChange(value: Boolean) {
        isFreeEvent = value
        if (value) {
            priceAmount = ""
        }
        checkForChanges()
    }

    fun onPriceAmountChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d+(\\.\\d{0,2})?$"))) {
            priceAmount = value
            checkForChanges()
        }
    }

    fun onPriceCurrencyChange(value: String) {
        priceCurrency = value
        checkForChanges()
    }

    fun onBackPressed() {
        if (hasUnsavedChanges) {
            showExitDialog = true
        }
    }

    fun dismissExitDialog() {
        showExitDialog = false
    }

    fun confirmExit(onExit: () -> Unit) {
        showExitDialog = false
        onExit()
    }

    private fun checkForChanges() {
        if (!isEditMode) {
            hasUnsavedChanges = title.isNotBlank() ||
                    description.isNotBlank() ||
                    locationAddress.isNotBlank() ||
                    maxAttendees.isNotBlank() ||
                    imageUrl.isNotBlank() ||
                    selectedImageUri != null ||
                    priceAmount.isNotBlank()
            return
        }

        val original = originalEvent ?: return
        hasUnsavedChanges = title != original.title ||
                description != (original.description ?: "") ||
                selectedCategory != original.category ||
                selectedDate != original.dateTime.toLocalDate() ||
                selectedTime != original.dateTime.toLocalTime() ||
                selectedEndDate != original.endDateTime?.toLocalDate() ||
                selectedEndTime != original.endDateTime?.toLocalTime() ||
                maxAttendees != (original.maxAttendees?.toString() ?: "") ||
                locationAddress != (original.location.address ?: "") ||
                imageUrl != (original.imageUrl ?: "") ||
                selectedImageUri != null ||
                isFreeEvent != (original.price?.isFree ?: true) ||
                priceAmount != (if (original.price?.isFree == false) original.price?.min?.toString() ?: "" else "")
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            locationService.getCurrentLocation().fold(
                onSuccess = { location ->
                    currentLocation = location
                    if (useCurrentLocation && !isEditMode) {
                        locationAddress = location.formatAddress()
                    }
                },
                onFailure = {
                    currentLocation = null
                }
            )
        }
    }

    override fun createEvent(onSuccess: () -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            uiState = UiState.Error("User not authenticated")
            return
        }

        viewModelScope.launch {
            uiState = UiState.Loading

            val location = if (useCurrentLocation && currentLocation != null) {
                currentLocation!!
            } else if (locationAddress.isNotBlank()) {
                locationService.geocodeAddress(locationAddress).getOrElse { error ->
                    uiState = UiState.Error("Could not find address: ${error.message}")
                    return@launch
                }
            } else {
                uiState = UiState.Error("Please provide a valid location")
                return@launch
            }

            val uploadedImageUrl = if (selectedImageUri != null) {
                isUploadingImage = true
                val compressedFile = compressImage(getApplication(), selectedImageUri!!)
                val uriToUpload = if (compressedFile != null) {
                    Uri.fromFile(compressedFile)
                } else {
                    selectedImageUri!!
                }

                imageUploadService.uploadImage(uriToUpload).getOrElse { error ->
                    isUploadingImage = false
                    uiState = UiState.Error("Failed to upload image: ${error.message}")
                    return@launch
                }.also {
                    isUploadingImage = false
                }
            } else {
                imageUrl.takeIf { it.isNotBlank() }
            }

            val dateTime = LocalDateTime.of(selectedDate, selectedTime)
            val endDateTime = if (selectedEndDate != null && selectedEndTime != null) {
                LocalDateTime.of(selectedEndDate, selectedEndTime)
            } else null

            val maxAttendeesInt = maxAttendees.toIntOrNull()

            val price = if (isFreeEvent) {
                Price.free()
            } else if (priceAmount.isNotBlank()) {
                val amount = priceAmount.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    Price.single(amount, priceCurrency)
                } else {
                    uiState = UiState.Error("Please provide a valid price amount")
                    return@launch
                }
            } else {
                null
            }

            if (isEditMode && editingEventId != null) {
                updateUserEventUseCase(
                    eventId = editingEventId!!,
                    newTitle = title,
                    newDescription = description.takeIf { it.isNotBlank() },
                    newCategory = selectedCategory,
                    newLocation = location,
                    newDateTime = dateTime,
                    newEndDateTime = endDateTime,
                    newMaxAttendees = maxAttendeesInt,
                    newImageUrl = uploadedImageUrl,
                    newPrice = price
                ).fold(
                    onSuccess = {
                        uiState = UiState.Success
                        hasUnsavedChanges = false
                        onSuccess()
                    },
                    onFailure = { exception ->
                        uiState = UiState.Error(handleException(exception))
                    }
                )
            } else {
                createUserEventUseCase(
                    title = title,
                    description = description.takeIf { it.isNotBlank() },
                    location = location,
                    dateTime = dateTime,
                    endDateTime = endDateTime,
                    category = selectedCategory,
                    organizerId = userId,
                    maxAttendees = maxAttendeesInt,
                    imageUrl = uploadedImageUrl,
                    price = price
                ).fold(
                    onSuccess = {
                        uiState = UiState.Success
                        hasUnsavedChanges = false
                        onSuccess()
                    },
                    onFailure = { exception ->
                        uiState = UiState.Error(handleException(exception))
                    }
                )
            }
        }
    }

    override fun clearError() {
        uiState = UiState.Idle
    }

    private fun getCurrentUserId(): UUID? {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return null
        return UUID.parse(firebaseUser.uid).getOrNull()
    }

    private fun handleException(exception: Throwable): String {
        return when (exception) {
            is InvalidEventException -> exception.message ?: "Invalid event data"
            else -> "Error saving event: ${exception.message}"
        }
    }

    private fun Location.formatAddress(): String {
        return address ?: "Lat: $latitude, Lng: $longitude"
    }
}
