package software.ulpgc.wherewhen.presentation.events.individual

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventSource
import software.ulpgc.wherewhen.domain.model.events.EventStatus
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: JetpackComposeEventDetailViewModel,
    eventId: UUID,
    onNavigateBack: () -> Unit,
    onEditEvent: (UUID) -> Unit = {},
    onEventDeleted: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(viewModel.inlineErrorMessage) {
        val message = viewModel.inlineErrorMessage
        if (message != null && message.isNotBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    if (viewModel.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Delete event") },
            text = {
                Text("Are you sure you want to delete this event? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissDeleteDialog()
                        viewModel.onDeleteEvent {
                            onEventDeleted()
                        }
                    }
                ) {
                    Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    val uiState = viewModel.uiState

    if (uiState is JetpackComposeEventDetailViewModel.UiState.Success && viewModel.showStatusDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissStatusDialog() },
            title = { Text("Change Event Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Select the new status for your event:",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    val selectableStatuses = listOf(
                        EventStatus.ACTIVE,
                        EventStatus.CANCELLED,
                        EventStatus.POSTPONED,
                        EventStatus.COMPLETED
                    )

                    selectableStatuses.forEach { status ->
                        val icon: ImageVector
                        val label: String
                        val color: Color
                        when (status) {
                            EventStatus.ACTIVE -> {
                                icon = Icons.Default.CheckCircle
                                label = "Active"
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            }

                            EventStatus.CANCELLED -> {
                                icon = Icons.Default.Close
                                label = "Cancelled"
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error
                            }

                            EventStatus.POSTPONED -> {
                                icon = Icons.Default.Schedule
                                label = "Postponed"
                                color = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                            }

                            EventStatus.COMPLETED -> {
                                icon = Icons.Default.Check
                                label = "Completed"
                                color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                            }

                            else -> {
                                icon = Icons.Default.Info
                                label = status.name.lowercase().replaceFirstChar { it.uppercase() }
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            }
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onUpdateStatus(status)
                                },
                            border = if (uiState.event.status == status) {
                                CardDefaults.outlinedCardBorder().copy(width = 2.dp)
                            } else {
                                CardDefaults.outlinedCardBorder()
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = color
                                )

                                Text(
                                    text = label,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                    color = if (uiState.event.status == status) {
                                        androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    } else {
                                        androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                    }
                                )

                                if (uiState.event.status == status) {
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "Current",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissStatusDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState is JetpackComposeEventDetailViewModel.UiState.Success &&
        viewModel.showAttendeesDialog
    ) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissAttendeesDialog() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Attendees",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "${uiState.attendees.size} ${if (uiState.attendees.size == 1) "person" else "people"} going",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.dismissAttendeesDialog() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                if (uiState.attendees.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No attendees yet",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        items(uiState.attendees) { attendee ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.dismissAttendeesDialog()
                                    }
                            ) {
                                if (attendee.profileImageUrl != null) {
                                    AsyncImage(
                                        model = attendee.profileImageUrl,
                                        contentDescription = "Profile picture",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(androidx.compose.material3.MaterialTheme.shapes.medium),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(androidx.compose.material3.MaterialTheme.shapes.medium)
                                            .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = attendee.name,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        if (attendee.isOrganizer) {
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        "Organizer",
                                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }

                                        if (attendee.isCurrentUser) {
                                            AssistChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        "You",
                                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        when (val state = viewModel.uiState) {
            is JetpackComposeEventDetailViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is JetpackComposeEventDetailViewModel.UiState.Success -> {
                EventDetailContent(
                    event = state.event,
                    isAttending = state.isAttending,
                    attendeesCount = state.attendees.size,
                    isOrganizer = state.isOrganizer,
                    isFull = state.isFull,
                    organizerName = state.organizerName,
                    isJoining = viewModel.isJoining,
                    onJoinEvent = { viewModel.onJoinEvent() },
                    onLeaveEvent = { viewModel.onLeaveEvent() },
                    onEditEvent = { onEditEvent(state.event.id) },
                    onDeleteEvent = { viewModel.showDeleteConfirmation() },
                    onChangeStatus = { viewModel.showStatusDialog() },
                    onShowAttendees = { viewModel.openAttendeesDialog() },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is JetpackComposeEventDetailViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )

                        Button(onClick = { viewModel.loadEvent(eventId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventDetailContent(
    event: Event,
    isAttending: Boolean,
    attendeesCount: Int,
    isOrganizer: Boolean,
    isFull: Boolean,
    organizerName: String?,
    isJoining: Boolean,
    onJoinEvent: () -> Unit,
    onLeaveEvent: () -> Unit,
    onEditEvent: () -> Unit,
    onDeleteEvent: () -> Unit,
    onChangeStatus: () -> Unit,
    onShowAttendees: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        event.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.title,
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )

                if (event.status != EventStatus.ACTIVE) {
                    val statusIcon: ImageVector
                    val statusLabel: String
                    val containerColor: Color
                    val labelColor: Color

                    when (event.status) {
                        EventStatus.CANCELLED -> {
                            statusIcon = Icons.Default.Close
                            statusLabel = "Cancelled"
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                            labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                        }

                        EventStatus.POSTPONED -> {
                            statusIcon = Icons.Default.Schedule
                            statusLabel = "Postponed"
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                            labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        EventStatus.COMPLETED -> {
                            statusIcon = Icons.Default.Check
                            statusLabel = "Completed"
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.tertiaryContainer
                            labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onTertiaryContainer
                        }

                        EventStatus.RESCHEDULED -> {
                            statusIcon = Icons.Default.DateRange
                            statusLabel = "Rescheduled"
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                            labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        else -> {
                            statusIcon = Icons.Default.CheckCircle
                            statusLabel = "Active"
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                            labelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    }

                    AssistChip(
                        onClick = {},
                        label = { Text(statusLabel) },
                        leadingIcon = { Icon(statusIcon, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = containerColor,
                            labelColor = labelColor
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = event.dateTime.format(
                            DateTimeFormatter.ofPattern(
                                "EEEE, dd MMMM yyyy 'at' HH:mm",
                                Locale.ENGLISH
                            )
                        ),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )

                    event.endDateTime?.let { endDate ->
                        Text(
                            text = "Until ${endDate.format(
                                DateTimeFormatter.ofPattern(
                                    "dd MMM yyyy 'at' HH:mm",
                                    Locale.ENGLISH
                                )
                            )}",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.location.formatAddress()
                            .takeIf { it.isNotEmpty() } ?: "Location not available",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    )

                    event.distance?.let { distance ->
                        Text(
                            text = "${String.format("%.1f", distance)} km away",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (event.location.latitude != null && event.location.longitude != null) {
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        val uri = android.net.Uri.parse(
                            "geo:${event.location.latitude},${event.location.longitude}" +
                                    "?q=${event.location.latitude},${event.location.longitude}(${event.title})"
                        )
                        val intent =
                            android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View location in Maps")
                }
            }

            event.price?.let { price ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (event.isFree()) Icons.Default.CheckCircle else Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = if (event.isFree()) {
                            androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.primary
                        }
                    )

                    Text(
                        text = price.formatPrice(),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        color = if (event.isFree()) {
                            androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                        } else {
                            androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            if (event.source == EventSource.USER_CREATED) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$attendeesCount going",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
                            )

                            if (attendeesCount > 0) {
                                FilledTonalButton(
                                    onClick = onShowAttendees,
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 4.dp
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "View",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }

                        organizerName?.let { name ->
                            Text(
                                text = "Organized by $name",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isFull && event.maxAttendees != null) {
                            Text(
                                text = "Event is full (${event.maxAttendees} max)",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(event.category.name) },
                    leadingIcon = {
                        Icon(Icons.Default.Star, contentDescription = null)
                    }
                )

                Text(
                    text = "Created ${event.createdAt.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
                    )}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            if (event.source == EventSource.EXTERNAL_API) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Text(
                            text = "Ticketmaster external event",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            event.description?.let { description ->
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Description",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = description,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            }

            event.externalUrl?.let { url ->
                val context = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url)
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("See at Ticketmaster")
                }
            }

            if (event.source == EventSource.USER_CREATED) {
                if (isOrganizer) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEditEvent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Create,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit")
                            }

                            OutlinedButton(
                                onClick = onChangeStatus,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.ManageHistory,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Status")
                            }
                        }

                        OutlinedButton(
                            onClick = onDeleteEvent,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Event")
                        }
                    }
                } else {
                    val canJoin = (event.status == EventStatus.ACTIVE || event.status == EventStatus.RESCHEDULED) && !isFull
                    val canLeave = isAttending
                    val enabled = !isJoining && (canJoin || canLeave)

                    Button(
                        onClick = {
                            if (isAttending) {
                                onLeaveEvent()
                            } else if (canJoin) {
                                onJoinEvent()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        colors = if (isAttending) {
                            ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                when {
                                    isAttending -> Icons.Default.Clear
                                    canJoin -> Icons.Default.Check
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    isAttending -> "Leave event"
                                    !canJoin && event.status == EventStatus.CANCELLED -> "Event cancelled"
                                    !canJoin && event.status == EventStatus.POSTPONED -> "Event postponed"
                                    !canJoin && event.status == EventStatus.COMPLETED -> "Event completed"
                                    !canJoin && event.status == EventStatus.RESCHEDULED -> "Event rescheduled"
                                    isFull -> "Event is full"
                                    else -> "Join event"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
