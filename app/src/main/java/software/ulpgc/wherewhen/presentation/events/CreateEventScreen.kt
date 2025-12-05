package software.ulpgc.wherewhen.presentation.events

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Parcelable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.parcelize.Parcelize
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

@Parcelize
data class UriWrapper(val uri: Uri) : Parcelable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: JetpackComposeCreateEventViewModel,
    eventIdToEdit: UUID? = null,
    onNavigateBack: () -> Unit,
    onEventCreated: () -> Unit
) {
    val context = LocalContext.current
    var tempPhotoUri by rememberSaveable { mutableStateOf<UriWrapper?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.uri?.let { viewModel.onImageSelected(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File.createTempFile(
                "event_photo_",
                ".jpg",
                context.externalCacheDir  // CAMBIADO: context.cacheDir -> context.externalCacheDir
            )
            val uri = FileProvider.getUriForFile(
                context,
                "software.ulpgc.wherewhen.fileprovider",
                photoFile
            )
            tempPhotoUri = UriWrapper(uri)
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(eventIdToEdit) {
        if (eventIdToEdit != null) {
            viewModel.loadEventToEdit(eventIdToEdit)
        } else {
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (eventIdToEdit != null) "Edit Event" else "Create Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Event Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            var expandedCategory by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = viewModel.selectedCategory.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false }
                ) {
                    EventCategory.values().forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.onCategoryChange(category)
                                expandedCategory = false
                            }
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            viewModel.onDateChange(LocalDate.of(year, month + 1, day))
                        },
                        viewModel.selectedDate.year,
                        viewModel.selectedDate.monthValue - 1,
                        viewModel.selectedDate.dayOfMonth
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: ${viewModel.selectedDate}")
            }

            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            viewModel.onTimeChange(LocalTime.of(hour, minute))
                        },
                        viewModel.selectedTime.hour,
                        viewModel.selectedTime.minute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Time: ${viewModel.selectedTime}")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Current Location")
                Switch(
                    checked = viewModel.useCurrentLocation,
                    onCheckedChange = { viewModel.onUseCurrentLocationChange(it) }
                )
            }

            OutlinedTextField(
                value = viewModel.locationAddress,
                onValueChange = { viewModel.onLocationAddressChange(it) },
                label = { Text("Location Address") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.useCurrentLocation
            )

            OutlinedTextField(
                value = viewModel.maxAttendees,
                onValueChange = { viewModel.onMaxAttendeesChange(it) },
                label = { Text("Max Attendees (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Event Image",
                style = MaterialTheme.typography.titleMedium
            )

            if (viewModel.selectedImageUri != null || viewModel.imageUrl.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box {
                        AsyncImage(
                            model = viewModel.selectedImageUri ?: viewModel.imageUrl,
                            contentDescription = "Event image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                viewModel.onImageSelected(null)
                                viewModel.onImageUrlChange("")
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gallery")
                }

                OutlinedButton(
                    onClick = {
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) -> {
                                val photoFile = File.createTempFile(
                                    "event_photo_",
                                    ".jpg",
                                    context.externalCacheDir  // CAMBIADO: context.cacheDir -> context.externalCacheDir
                                )
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "software.ulpgc.wherewhen.fileprovider",
                                    photoFile
                                )
                                tempPhotoUri = UriWrapper(uri)
                                cameraLauncher.launch(uri)
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Camera")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Free Event")
                Switch(
                    checked = viewModel.isFreeEvent,
                    onCheckedChange = { viewModel.onIsFreeEventChange(it) }
                )
            }

            if (!viewModel.isFreeEvent) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = viewModel.priceAmount,
                        onValueChange = { viewModel.onPriceAmountChange(it) },
                        label = { Text("Price") },
                        modifier = Modifier.weight(2f),
                        placeholder = { Text("10.50") }
                    )

                    var expandedCurrency by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCurrency,
                        onExpandedChange = { expandedCurrency = !expandedCurrency },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = viewModel.priceCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            listOf("EUR", "USD", "GBP").forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        viewModel.onPriceCurrencyChange(currency)
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (viewModel.uiState is JetpackComposeCreateEventViewModel.UiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (viewModel.uiState as JetpackComposeCreateEventViewModel.UiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Button(
                onClick = { viewModel.createEvent(onEventCreated) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.uiState !is JetpackComposeCreateEventViewModel.UiState.Loading && !viewModel.isUploadingImage
            ) {
                if (viewModel.uiState is JetpackComposeCreateEventViewModel.UiState.Loading || viewModel.isUploadingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (viewModel.isUploadingImage) "Uploading image..." else "Saving...")
                } else {
                    Text(if (eventIdToEdit != null) "Update Event" else "Create Event")
                }
            }
        }
    }
}
