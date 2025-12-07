package software.ulpgc.wherewhen.presentation.events.form

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
import android.app.Activity
import com.yalantis.ucrop.UCrop


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

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let { uri ->
                viewModel.onImageSelected(uri)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = result.data?.let { UCrop.getError(it) }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            val destinationUri = Uri.fromFile(
                File(
                    context.cacheDir,
                    "event_cropped_${System.currentTimeMillis()}.jpg"
                )
            )

            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(1000, 1000)

            cropLauncher.launch(uCrop.getIntent(context))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.uri?.let { sourceUri ->
                val destinationUri = Uri.fromFile(
                    File(
                        context.cacheDir,
                        "event_cropped_${System.currentTimeMillis()}.jpg"
                    )
                )

                val uCrop = UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1f, 1f)
                    .withMaxResultSize(1000, 1000)

                cropLauncher.launch(uCrop.getIntent(context))
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File.createTempFile(
                "event_photo_",
                ".jpg",
                context.externalCacheDir
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
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = viewModel.title,
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Event Title *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("Description") },
                placeholder = { Text("Tell people what your event is about...") },
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Date & Time",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Date", style = MaterialTheme.typography.labelSmall)
                        Text(viewModel.selectedDate.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
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
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Time", style = MaterialTheme.typography.labelSmall)
                        Text(viewModel.selectedTime.toString(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            var hasEndDateTime by remember { mutableStateOf(viewModel.selectedEndDate != null) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set End Date/Time")
                Switch(
                    checked = hasEndDateTime,
                    onCheckedChange = {
                        hasEndDateTime = it
                        if (!it) {
                            viewModel.onEndDateChange(null)
                            viewModel.onEndTimeChange(null)
                        } else {
                            viewModel.onEndDateChange(viewModel.selectedDate.plusDays(1))
                            viewModel.onEndTimeChange(viewModel.selectedTime)
                        }
                    }
                )
            }

            if (hasEndDateTime) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    viewModel.onEndDateChange(LocalDate.of(year, month + 1, day))
                                },
                                viewModel.selectedEndDate?.year ?: viewModel.selectedDate.year,
                                (viewModel.selectedEndDate?.monthValue ?: viewModel.selectedDate.monthValue) - 1,
                                viewModel.selectedEndDate?.dayOfMonth ?: viewModel.selectedDate.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("End Date", style = MaterialTheme.typography.labelSmall)
                            Text(
                                viewModel.selectedEndDate?.toString() ?: "Not set",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    viewModel.onEndTimeChange(LocalTime.of(hour, minute))
                                },
                                viewModel.selectedEndTime?.hour ?: viewModel.selectedTime.hour,
                                viewModel.selectedEndTime?.minute ?: viewModel.selectedTime.minute,
                                true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("End Time", style = MaterialTheme.typography.labelSmall)
                            Text(
                                viewModel.selectedEndTime?.toString() ?: "Not set",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Location",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

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
                placeholder = { Text("Enter address or venue name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.useCurrentLocation
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Capacity & Pricing",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = viewModel.maxAttendees,
                onValueChange = { viewModel.onMaxAttendeesChange(it) },
                label = { Text("Max Attendees") },
                placeholder = { Text("Leave empty for unlimited") },
                modifier = Modifier.fillMaxWidth()
            )

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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Event Image",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
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
                                    context.externalCacheDir
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
