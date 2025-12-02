package software.ulpgc.wherewhen.presentation.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import software.ulpgc.wherewhen.domain.model.events.Event
import software.ulpgc.wherewhen.domain.model.events.EventCategory
import software.ulpgc.wherewhen.domain.valueObjects.UUID
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    viewModel: JetpackComposeEventsViewModel,
    onEventClick: (UUID) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                actions = {
                    IconButton(onClick = { viewModel.onRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = viewModel.selectedTab) {
                Tab(
                    selected = viewModel.selectedTab == 0,
                    onClick = { viewModel.onTabSelected(0) },
                    text = { Text("Nearby") }
                )
                Tab(
                    selected = viewModel.selectedTab == 1,
                    onClick = { viewModel.onTabSelected(1) },
                    text = { Text("My events") }
                )
            }

            if (viewModel.selectedTab == 0) {
                SearchBar(
                    query = viewModel.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    onClearSearch = { viewModel.clearSearch() }
                )

                RadiusControl(
                    radiusKm = viewModel.radiusKm,
                    onRadiusChange = { viewModel.onRadiusChange(it) }
                )

                CategoryFilter(
                    selectedCategory = viewModel.selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) }
                )
            }

            when (val state = viewModel.uiState) {
                is JetpackComposeEventsViewModel.UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is JetpackComposeEventsViewModel.UiState.Success -> {
                    if (state.events.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (viewModel.selectedTab == 0)
                                    "There are no nearby events"
                                else
                                    "You have not got any joined events",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        EventsList(
                            events = state.events,
                            onEventClick = onEventClick
                        )
                    }
                }
                is JetpackComposeEventsViewModel.UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.onRefresh() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is JetpackComposeEventsViewModel.UiState.Idle -> {}
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Search events...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Clear, contentDescription = "Clean search")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun RadiusControl(
    radiusKm: Int,
    onRadiusChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search radius",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$radiusKm km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = radiusKm.toFloat(),
            onValueChange = { onRadiusChange(it.toInt()) },
            valueRange = 5f..100f,
            steps = 18,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: EventCategory?,
    onCategorySelected: (EventCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        items(EventCategory.values()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.name) }
            )
        }
    }
}

@Composable
private fun EventsList(
    events: List<Event>,
    onEventClick: (UUID) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(events) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event.id) }
            )
        }
    }
}

@Composable
private fun EventCard(
    event: Event,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            event.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.location.formatAddress().takeIf { it.isNotEmpty() } ?: "Location not available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = event.dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
