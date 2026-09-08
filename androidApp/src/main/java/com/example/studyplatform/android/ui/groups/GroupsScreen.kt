package com.example.studyplatform.android.ui.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studyplatform.android.components.LoadingScreen
import com.example.studyplatform.android.components.PrimaryButton
import com.example.studyplatform.android.components.SecondaryButton
import com.example.studyplatform.android.components.OfflineBanner
import com.example.studyplatform.android.theme.*
import com.example.studyplatform.data.AppData
import com.example.studyplatform.data.Offline
import com.example.studyplatform.api.AppJson
import com.example.studyplatform.api.ChatApi
import com.example.studyplatform.api.GroupApi
import com.example.studyplatform.api.StompClient
import com.example.studyplatform.model.*
import kotlinx.coroutines.launch

@Composable
fun GroupListScreen(onGroupClick: (String) -> Unit) {
    var groups by remember { mutableStateOf<List<GroupResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var fromCache by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            val result = AppData.library.groups()
            groups = result.value
            fromCache = result.fromCache
        }
    }
    LaunchedEffect(Unit) { refresh(); loading = false }
    LaunchedEffect(error) { if (error != null) { snackbar.showSnackbar(error!!); error = null } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Background
    ) { padding ->
        if (loading) { LoadingScreen(); return@Scaffold }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text("Study Groups", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { showCreate = true; showJoin = false }, Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Create")
                    }
                    OutlinedButton(onClick = { showJoin = true; showCreate = false }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Join")
                    }
                }
            }

            if (showCreate) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SuccessLight), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Create a Group", fontWeight = FontWeight.SemiBold, color = Success)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group name") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Success, unfocusedBorderColor = Border))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optional)") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Success, unfocusedBorderColor = Border))
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrimaryButton(text = "Create", loading = creating, enabled = name.isNotBlank(), modifier = Modifier.weight(1f),
                                    onClick = { creating = true; scope.launch {
                                        try { GroupApi.create(CreateGroupRequest(name, desc.ifBlank { null })); refresh(); showCreate = false; name = ""; desc = "" }
                                        catch (e: Exception) { error = e.message }
                                        creating = false
                                    }})
                                TextButton(onClick = { showCreate = false }) { Text("Cancel", color = TextMuted) }
                            }
                        }
                    }
                }
            }

            if (showJoin) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PrimaryLight), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Join a Group", fontWeight = FontWeight.SemiBold, color = Primary)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = inviteCode, onValueChange = { inviteCode = it }, label = { Text("Invite code") },
                                placeholder = { Text("e.g. GRP789ABC") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border))
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PrimaryButton(text = "Join", loading = creating, enabled = inviteCode.isNotBlank(), modifier = Modifier.weight(1f),
                                    onClick = { creating = true; scope.launch {
                                        try { GroupApi.join(JoinGroupRequest(inviteCode.trim())); refresh(); showJoin = false; inviteCode = "" }
                                        catch (e: Exception) { error = e.message }
                                        creating = false
                                    }})
                                TextButton(onClick = { showJoin = false }) { Text("Cancel", color = TextMuted) }
                            }
                        }
                    }
                }
            }

            if (groups.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Group, null, Modifier.size(48.dp), tint = TextMuted)
                            Spacer(Modifier.height(8.dp))
                            Text("No groups yet", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Create or join a study group", fontSize = 13.sp, color = TextMuted)
                        }
                    }
                }
            }

            items(groups) { group ->
                val clipboard = LocalClipboardManager.current
                Card(onClick = { onGroupClick(group.id) }, modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(group.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(group.myRole ?: "", fontSize = 11.sp, color = if (group.myRole == "ADMIN") Primary else TextMuted, fontWeight = FontWeight.Medium)
                        }
                        if (group.description != null) { Spacer(Modifier.height(4.dp)); Text(group.description!!, fontSize = 13.sp, color = TextSecondary) }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${group.memberCount} members", fontSize = 12.sp, color = TextMuted)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { clipboard.setText(AnnotatedString(group.inviteCode)) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = Primary)
                                Spacer(Modifier.width(4.dp))
                                Text("Copy code", fontSize = 12.sp, color = Primary)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun GroupDetailScreen(groupId: String, onBack: () -> Unit) {
    var group by remember { mutableStateOf<GroupResponse?>(null) }
    var members by remember { mutableStateOf<List<GroupMemberResponse>>(emptyList()) }
    var messages by remember { mutableStateOf<List<ChatMessageResponse>>(emptyList()) }
    var newMessage by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    var fromCache by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        // Membership and past messages read from the cache when there is no connection.
        // Sending a message does not: a message that only exists on one phone is a
        // promise the app cannot keep, and a conversation has no sensible conflict
        // resolution. The composer says so instead of queueing silently.
        val groupResult = AppData.library.group(groupId)
        val memberResult = AppData.library.members(groupId)
        val chatResult = AppData.library.chat(groupId)

        group = groupResult.value
        members = memberResult.value
        messages = chatResult.value
        fromCache = groupResult.fromCache || memberResult.fromCache || chatResult.fromCache
        loading = false
    }

    // Live messages. Separate from the load above so a socket that cannot open never
    // stops the history from rendering — the screen degrades to what REST already
    // fetched rather than showing nothing.
    //
    // Deduplicated by id because the same message can arrive twice: once as the reply
    // to our own REST send, once as the broadcast every subscriber receives.
    LaunchedEffect(groupId) {
        runCatching {
            StompClient.subscribe("/topic/chat/$groupId").collect { body ->
                val incoming = runCatching {
                    AppJson.instance.decodeFromString(ChatMessageResponse.serializer(), body)
                }.getOrNull() ?: return@collect

                if (messages.none { it.id == incoming.id }) {
                    messages = messages + incoming
                }
            }
        }.onFailure { println("Chat socket closed: ${it.message}") }
    }

    if (loading) { LoadingScreen(); return }
    val g = group ?: return

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("< Back", color = Primary) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(g.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${g.memberCount} members", fontSize = 13.sp, color = TextMuted)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Tabs
        TabRow(selectedTabIndex = selectedTab, containerColor = Surface, contentColor = Primary,
            indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = Primary) }) {
            listOf("Chat", "Members").forEachIndexed { idx, title ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx },
                    text = { Text(title, fontWeight = if (selectedTab == idx) FontWeight.SemiBold else FontWeight.Normal) })
            }
        }

        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Chat
                LazyColumn(Modifier.weight(1f), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages.reversed()) { msg ->
                        Column {
                            Text(msg.senderName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                            Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(10.dp)) {
                                Text(msg.content, Modifier.padding(10.dp), fontSize = 14.sp, color = TextSecondary)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newMessage, onValueChange = { newMessage = it },
                        placeholder = { Text("Type a message...") }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary, unfocusedBorderColor = Border))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        if (newMessage.isNotBlank()) {
                            val msg = newMessage; newMessage = ""
                            scope.launch {
                                try {
                                    val sent = ChatApi.send(ChatMessageRequest(groupId, msg))
                                    // The broadcast may beat this reply back to us.
                                    if (messages.none { it.id == sent.id }) {
                                        messages = messages + sent
                                    }
                                } catch (_: Exception) {
                                    // Put the text back rather than losing what they
                                    // typed: sending is online-only and this is how
                                    // they find out.
                                    newMessage = msg
                                }
                            }
                        }
                    }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text("Send")
                    }
                }
            }
            1 -> {
                // Members
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(members) { m ->
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${m.firstName} ${m.lastName}", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(m.email, fontSize = 12.sp, color = TextMuted)
                                }
                                Text(m.role, fontSize = 11.sp, color = if (m.role == "ADMIN") Primary else TextMuted,
                                    fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
