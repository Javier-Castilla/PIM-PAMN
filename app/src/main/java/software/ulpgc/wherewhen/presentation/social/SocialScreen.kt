package software.ulpgc.wherewhen.presentation.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import software.ulpgc.wherewhen.domain.model.user.User
import software.ulpgc.wherewhen.domain.usecases.friendship.FriendshipStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    viewModel: JetpackComposeSocialViewModel,
    onMessageClick: (User) -> Unit = {}
) {
    val uiState = viewModel.uiState
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> viewModel.clearSearch()
            1 -> viewModel.loadPendingRequests()
            2 -> viewModel.loadFriends()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Social") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Search") },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Requests") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.receivedRequests.isNotEmpty()) {
                                    Badge { Text("${uiState.receivedRequests.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Friends") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> SearchTab(viewModel, uiState)
                1 -> RequestsTab(viewModel, uiState)
                2 -> FriendsTab(viewModel, uiState, onMessageClick)
            }
        }
    }
}

@Composable
fun SearchTab(viewModel: JetpackComposeSocialViewModel, uiState: SocialUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text("Search users") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            uiState.users.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                Text(
                    "No users found",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    items(uiState.users) { userWithStatus ->
                        UserCard(
                            userWithStatus = userWithStatus,
                            onAddClick = { viewModel.sendFriendRequest(userWithStatus.user.uuid) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestsTab(viewModel: JetpackComposeSocialViewModel, uiState: SocialUiState) {
    if (uiState.receivedRequests.isEmpty() && uiState.sentRequests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending requests")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (uiState.receivedRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Received",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(uiState.receivedRequests) { requestWithUser ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (requestWithUser.user.profileImageUrl != null) {
                                AsyncImage(
                                    model = requestWithUser.user.profileImageUrl,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = requestWithUser.user.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Wants to be your friend",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.acceptFriendRequest(requestWithUser.request.id) }
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Accept",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.rejectFriendRequest(requestWithUser.request.id) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Reject",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.sentRequests.isNotEmpty()) {
                item {
                    Text(
                        text = "Sent",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(
                            top = if (uiState.receivedRequests.isNotEmpty()) 16.dp else 8.dp,
                            bottom = 8.dp
                        )
                    )
                }
                items(uiState.sentRequests) { requestWithUser ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (requestWithUser.user.profileImageUrl != null) {
                                AsyncImage(
                                    model = requestWithUser.user.profileImageUrl,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = requestWithUser.user.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Waiting for response",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { viewModel.cancelFriendRequest(requestWithUser.request.id) }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FriendsTab(
    viewModel: JetpackComposeSocialViewModel,
    uiState: SocialUiState,
    onMessageClick: (User) -> Unit
) {
    if (uiState.friendToRemove != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRemoveFriendDialog() },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove ${uiState.friendToRemove.name} from your friends?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRemoveFriend() }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRemoveFriendDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.friends.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No friends yet")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(uiState.friends) { friend ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (friend.profileImageUrl != null) {
                            AsyncImage(
                                model = friend.profileImageUrl,
                                contentDescription = "Profile picture",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = friend.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onMessageClick(friend) }) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Send message",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { viewModel.showRemoveFriendDialog(friend) }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove friend",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserCard(userWithStatus: UserWithStatus, onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (userWithStatus.user.profileImageUrl != null) {
                AsyncImage(
                    model = userWithStatus.user.profileImageUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = userWithStatus.user.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            when (userWithStatus.status) {
                FriendshipStatus.NOT_FRIENDS -> {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Add friend",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                FriendshipStatus.REQUEST_SENT -> {
                    Text(
                        "Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                FriendshipStatus.REQUEST_RECEIVED -> {
                    Text(
                        "Awaiting response",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                FriendshipStatus.FRIENDS -> {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Friends",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
