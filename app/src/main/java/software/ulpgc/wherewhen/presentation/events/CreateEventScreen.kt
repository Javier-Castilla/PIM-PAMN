package software.ulpgc.wherewhen.presentation.events

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: JetpackComposeCreateEventViewModel,
    onNavigateBack: () -> Unit,
    onEventCreated: () -> Unit
) {
    LaunchedEffect(viewModel.uiState) {
        if (viewModel.uiState is JetpackComposeCreateEventViewModel.UiState.Success) {
            onEventCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") },
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
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )

            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.onDescriptionChange(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            CategoryDropdown(
                selectedCategory = viewModel.selectedCategory,
                onCategorySelected = { viewModel.onCategoryChange(it) }
            )

            Text(
                text = "Date and Time *",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateField(
                    date = viewModel.selectedDate,
                    onDateSelected = { viewModel.onDateChange(it) },
                    modifier = Modifier.weight(1f)
                )
                TimeField(
                    time = viewModel.selectedTime,
                    onTimeSelected = { viewModel.onTimeChange(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "End Date and Time (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (viewModel.selectedEndDate != null) {
                    TextButton(onClick = {
                        viewModel.onEndDateChange(null)
                        viewModel.onEndTimeChange(null)
                    }) {
                        Text("Clear")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateField(
                    date = viewModel.selectedEndDate ?: viewModel.selectedDate,
                    onDateSelected = { viewModel.onEndDateChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "End date"
                )
                TimeField(
                    time = viewModel.selectedEndTime ?: LocalTime.of(20, 0),
                    onTimeSelected = { viewModel.onEndTimeChange(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "End time"
                )
            }

            Text(
                text = "Location *",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = viewModel.useCurrentLocation,
                    onCheckedChange = { viewModel.onUseCurrentLocationChange(it) }
                )
                Text("Use current location")
            }

            OutlinedTextField(
                value = viewModel.locationAddress,
                onValueChange = { viewModel.onLocationAddressChange(it) },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.useCurrentLocation,
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                }
            )

            OutlinedTextField(
                value = viewModel.maxAttendees,
                onValueChange = { viewModel.onMaxAttendeesChange(it) },
                label = { Text("Max attendees (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.uiState is JetpackComposeCreateEventViewModel.UiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = (viewModel.uiState as JetpackComposeCreateEventViewModel.UiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Button(
                onClick = { viewModel.createEvent(onEventCreated) },
                modifier = Modifier.fillMaxWidth(),
                enabled = viewModel.uiState != JetpackComposeCreateEventViewModel.UiState.Loading
            ) {
                if (viewModel.uiState == JetpackComposeCreateEventViewModel.UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Event")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: EventCategory,
    onCategorySelected: (EventCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category *") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            leadingIcon = {
                Icon(Icons.Default.Star, contentDescription = null)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            EventCategory.values().forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DateField(
    date: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Date"
) {
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    OutlinedTextField(
        value = date.format(dateFormatter),
        onValueChange = {},
        readOnly = true,
        label = { Text(placeholder) },
        modifier = modifier.clickable {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        },
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = null)
        },
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun TimeField(
    time: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Time"
) {
    val context = LocalContext.current
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    OutlinedTextField(
        value = time.format(timeFormatter),
        onValueChange = {},
        readOnly = true,
        label = { Text(placeholder) },
        modifier = modifier.clickable {
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    onTimeSelected(LocalTime.of(hourOfDay, minute))
                },
                time.hour,
                time.minute,
                true
            ).show()
        },
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = null)
        },
        enabled = false,
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
