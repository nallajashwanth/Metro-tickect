package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.MetroViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MetroAppContainer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroAppContainer(viewModel: MetroViewModel = viewModel()) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val bookedTickets by viewModel.bookedTickets.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    
    // Dialog control states
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showActiveTripDialog by remember { mutableStateOf(false) }
    var selectedQrTicket by remember { mutableStateOf<BookedTicket?>(null) }
    var showRoutePreviewDialog by remember { mutableStateOf(false) }

    // Active Simulated Trip State
    val activeTripTicket by viewModel.activeTripTicket.collectAsStateWithLifecycle()

    // Trigger toast for UI messages
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUiMessage()
        }
    }

    // Edge-to-Edge window insets padding
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MetroTopAppBar(
                currentLang = appLanguage,
                onLangChange = { viewModel.changeLanguage(it) }
            )
        },
        bottomBar = {
            MetroBottomNavigation(
                currentTab = currentTab,
                onTabSelect = { viewModel.setTab(it) },
                lang = appLanguage
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    "BOOK" -> BookTicketScreen(
                        viewModel = viewModel,
                        lang = appLanguage,
                        profile = userProfile,
                        onAddMoneyClick = { showAddMoneyDialog = true }
                    )
                    "SCHEDULES" -> SchedulesScreen(
                        viewModel = viewModel,
                        lang = appLanguage,
                        onShowRoute = { showRoutePreviewDialog = true }
                    )
                    "PASSES" -> MyTicketsScreen(
                        tickets = bookedTickets,
                        lang = appLanguage,
                        onViewQr = { selectedQrTicket = it },
                        onStartTrip = { 
                            viewModel.startTripSimulation(it)
                            showActiveTripDialog = true
                        }
                    )
                    "PROFILE" -> ProfileScreen(
                        viewModel = viewModel,
                        profile = userProfile,
                        lang = appLanguage,
                        onAddMoneyClick = { showAddMoneyDialog = true }
                    )
                    "AI" -> AiAssistantScreen(
                        viewModel = viewModel,
                        lang = appLanguage
                    )
                }
            }

            // Floater UI indicator if trip simulation is active in background
            if (activeTripTicket != null && !showActiveTripDialog) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showActiveTripDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(24.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Train,
                            contentDescription = "Active Ride",
                            tint = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = Translations.getString("active_trip", appLanguage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // WALLET TOP UP DIALOG
    if (showAddMoneyDialog) {
        WalletTopUpDialog(
            lang = appLanguage,
            onDismiss = { showAddMoneyDialog = false },
            onConfirm = { amount ->
                viewModel.addMoney(amount)
                showAddMoneyDialog = false
            }
        )
    }

    // GATE QR TICKET DIALOG
    selectedQrTicket?.let { ticket ->
        QrTicketDialog(
            ticket = ticket,
            lang = appLanguage,
            onDismiss = { selectedQrTicket = null }
        )
    }

    // ACTIVE TRIP GPS SIMULATOR DIALOG
    if (showActiveTripDialog && activeTripTicket != null) {
        ActiveTripSimulatorDialog(
            viewModel = viewModel,
            lang = appLanguage,
            onDismiss = { showActiveTripDialog = false }
        )
    }

    // ROUTE PREVIEW DIALOG
    if (showRoutePreviewDialog) {
        RoutePreviewDialog(
            viewModel = viewModel,
            lang = appLanguage,
            onDismiss = { showRoutePreviewDialog = false }
        )
    }
}

// ---------------------- TOP APP BAR ----------------------
@Composable
fun MetroTopAppBar(
    currentLang: AppLanguage,
    onLangChange: (AppLanguage) -> Unit
) {
    Surface(
        tonalElevation = 0.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Train,
                            contentDescription = "Metro Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Translations.getString("app_title", currentLang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hyderabad Metro Rail",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Quick Multilingual Lang Selector Row
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = lang == currentLang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { onLangChange(lang) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (lang == AppLanguage.TELUGU) "తె" else if (lang == AppLanguage.HINDI) "हि" else "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.height(1.dp))
        }
    }
}

// ---------------------- BOTTOM NAV BAR ----------------------
@Composable
fun MetroBottomNavigation(
    currentTab: String,
    onTabSelect: (String) -> Unit,
    lang: AppLanguage
) {
    Column {
        Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.height(1.dp))
        NavigationBar(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            tonalElevation = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val items = listOf(
                Triple("BOOK", Icons.Default.ConfirmationNumber, "book_tickets"),
                Triple("SCHEDULES", Icons.Default.Map, "live_schedules"),
                Triple("PASSES", Icons.Default.QrCodeScanner, "my_passes"),
                Triple("AI", Icons.Default.SmartToy, "ai_assistant"),
                Triple("PROFILE", Icons.Default.AccountCircle, "user_profile")
            )

            items.forEach { (tabId, icon, labelKey) ->
                val isSelected = currentTab == tabId
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelect(tabId) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = Translations.getString(labelKey, lang)
                        )
                    },
                    label = {
                        Text(
                            text = Translations.getString(labelKey, lang),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_$tabId")
                )
            }
        }
    }
}

// ---------------------- SCREEN 1: TICKET BOOKING ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookTicketScreen(
    viewModel: MetroViewModel,
    lang: AppLanguage,
    profile: UserProfile?,
    onAddMoneyClick: () -> Unit
) {
    val source by viewModel.sourceStation.collectAsStateWithLifecycle()
    val dest by viewModel.destStation.collectAsStateWithLifecycle()
    val ticketType by viewModel.ticketType.collectAsStateWithLifecycle()

    var sourceExpanded by remember { mutableStateOf(false) }
    var destExpanded by remember { mutableStateOf(false) }

    // Path details calculations
    val route = remember(source, dest) { MetroNetwork.findRoute(source, dest) }
    val fare = remember(route, ticketType) {
        val base = route?.fare ?: 10.0
        when (ticketType) {
            "RETURN" -> base * 1.8
            "PASS" -> 120.00
            else -> base
        }
    }

    var showPaymentConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card with modern graphic style
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF1D4ED8))
                        )
                    )
                    .drawBehind {
                        // Minimalist overlapping train lines graphics
                        val pathWidth = size.width
                        val pathHeight = size.height
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = 120.dp.toPx(),
                            center = Offset(pathWidth, pathHeight / 2)
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.15f),
                            start = Offset(0f, pathHeight * 0.7f),
                            end = Offset(pathWidth, pathHeight * 0.2f),
                            strokeWidth = 6.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.1f),
                            start = Offset(0f, pathHeight * 0.8f),
                            end = Offset(pathWidth, pathHeight * 0.4f),
                            strokeWidth = 6.dp.toPx()
                        )
                    }
                    .padding(20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        text = if (lang == AppLanguage.TELUGU) "రైలు ప్రయాణం సులభతరం" else if (lang == AppLanguage.HINDI) "सुरक्षित और आसान यात्रा" else "Instant QR Tickets",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (lang == AppLanguage.TELUGU) "లైన్ మార్పు మరియు స్మార్ట్ ఛార్జీలతో" else if (lang == AppLanguage.HINDI) "स्मार्ट किराए और लाइव ट्रैकिंग के साथ" else "Seamless journey, interactive routes & live GPS announcements",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Wallet Balance Quick View
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = Translations.getString("wallet_balance", lang),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "₹${String.format("%.2f", profile?.walletBalance ?: 0.0)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Button(
                        onClick = onAddMoneyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("quick_add_cash")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Translations.getString("add_money", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Booking Selection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Source Dropdown Selection
                    Text(
                        text = Translations.getString("source", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = sourceExpanded,
                        onExpandedChange = { sourceExpanded = !sourceExpanded }
                    ) {
                        OutlinedTextField(
                            value = source,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("source_dropdown"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = sourceExpanded,
                            onDismissRequest = { sourceExpanded = false }
                        ) {
                            MetroNetwork.stations.sortedBy { it.name }.forEach { station ->
                                val dispName = when (lang) {
                                    AppLanguage.TELUGU -> station.nameTe
                                    AppLanguage.HINDI -> station.nameHi
                                    else -> station.name
                                }
                                DropdownMenuItem(
                                    text = { Text(dispName, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        viewModel.setSourceStation(station.name)
                                        sourceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Interchanging button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val s = source
                                viewModel.setSourceStation(dest)
                                viewModel.setDestStation(s)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = "Swap Stations",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Destination Dropdown Selection
                    Text(
                        text = Translations.getString("destination", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = destExpanded,
                        onExpandedChange = { destExpanded = !destExpanded }
                    ) {
                        OutlinedTextField(
                            value = dest,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = destExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("dest_dropdown"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = destExpanded,
                            onDismissRequest = { destExpanded = false }
                        ) {
                            MetroNetwork.stations.sortedBy { it.name }.forEach { station ->
                                val dispName = when (lang) {
                                    AppLanguage.TELUGU -> station.nameTe
                                    AppLanguage.HINDI -> station.nameHi
                                    else -> station.name
                                }
                                DropdownMenuItem(
                                    text = { Text(dispName, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        viewModel.setDestStation(station.name)
                                        destExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ticket Type Selector
                    Text(
                        text = Translations.getString("ticket_type", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SINGLE", "RETURN", "PASS").forEach { type ->
                            val isSel = ticketType == type
                            val label = when (type) {
                                "SINGLE" -> Translations.getString("single_journey", lang)
                                "RETURN" -> Translations.getString("return_journey", lang)
                                else -> Translations.getString("smart_pass", lang)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { viewModel.setTicketType(type) }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fare Estimate and Route Quick Info Card
        if (route != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trip Breakdown",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(onClick = { viewModel.setTab("SCHEDULES") }) {
                                Text("View Map Route", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    Translations.getString("distance", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${String.format("%.1f", route.distanceKm)} ${Translations.getString("km", lang)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    Translations.getString("duration", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${route.durationMin} ${Translations.getString("mins", lang)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    Translations.getString("fare", lang),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "₹${String.format("%.2f", fare)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (route.interchanges.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AltRoute,
                                    contentDescription = "Transfer",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lang == AppLanguage.TELUGU) "అమీర్‌పేట్ వద్ద బదిలీ" else if (lang == AppLanguage.HINDI) "अमीरपेट पर लाइन बदलें" else "Transfer lines at ${route.interchanges.first().name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action Booking Button
        item {
            Button(
                onClick = { showPaymentConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("book_ticket_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = "Pay"
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = Translations.getString("proceed_payment", lang) + " (₹${String.format("%.2f", fare)})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // SECURE SIMULATED PAYMENT CONFIRMATION DIALOG
    if (showPaymentConfirmDialog) {
        Dialog(onDismissRequest = { showPaymentConfirmDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Gateway",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Secure Metro Payment Gateway",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your booking from $source to $dest will deduct ₹${String.format("%.2f", fare)} from your Metro wallet.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Card simulated view
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Metro Card Pass", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Filled.Train, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = profile?.name ?: "Guest Rider",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Wallet Bal: ₹${String.format("%.2f", profile?.walletBalance ?: 0.0)}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPaymentConfirmDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(Translations.getString("cancel", lang), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showPaymentConfirmDialog = false
                                viewModel.bookTicketDirectly()
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("secure_pay_now"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(Translations.getString("confirm_booking", lang), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- SCREEN 2: LIVE SCHEDULES & ROUTES ----------------------
@Composable
fun SchedulesScreen(
    viewModel: MetroViewModel,
    lang: AppLanguage,
    onShowRoute: () -> Unit
) {
    val source by viewModel.sourceStation.collectAsStateWithLifecycle()
    val dest by viewModel.destStation.collectAsStateWithLifecycle()

    var activeLineTab by remember { mutableStateOf("RED") } // "RED", "BLUE", "GREEN"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Line Selectors Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            listOf("RED", "BLUE", "GREEN").forEach { line ->
                val isSel = activeLineTab == line
                val colorHex = when (line) {
                    "RED" -> Color(0xFFE53935)
                    "BLUE" -> Color(0xFF1E88E5)
                    else -> Color(0xFF4CAF50)
                }
                val label = when (line) {
                    "RED" -> if (lang == AppLanguage.TELUGU) "ఎరుపు" else if (lang == AppLanguage.HINDI) "लाल" else "Red Line"
                    "BLUE" -> if (lang == AppLanguage.TELUGU) "నీలం" else if (lang == AppLanguage.HINDI) "नीला" else "Blue Line"
                    else -> if (lang == AppLanguage.TELUGU) "ఆకుపచ్చ" else if (lang == AppLanguage.HINDI) "ఆకుపచ్చ" else "Green Line"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) colorHex.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSel) colorHex else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { activeLineTab = line }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSel) colorHex else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Inter-Route Preview Floating Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Translations.getString("route_recs", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$source → $dest",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onShowRoute,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("preview_path_button")
                ) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = "View Route")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Show Path", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vertical List of Stations for selected line
        val lineStations = remember(activeLineTab) {
            val selectedLine = when (activeLineTab) {
                "RED" -> MetroLine.RED
                "BLUE" -> MetroLine.BLUE
                else -> MetroLine.GREEN
            }
            MetroNetwork.stations.filter { it.line == selectedLine }.sortedBy { it.order }
        }

        Text(
            text = "Line Stops & Schedules (~2 mins interval)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(lineStations) { idx, station ->
                val colorHex = when (station.line) {
                    MetroLine.RED -> Color(0xFFE53935)
                    MetroLine.BLUE -> Color(0xFF1E88E5)
                    else -> Color(0xFF4CAF50)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Line node graphic
                    Box(
                        modifier = Modifier.width(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            // Draw vertical connector line
                            val lineLength = 40.dp.toPx()
                            if (idx > 0) {
                                drawLine(
                                    color = colorHex.copy(alpha = 0.5f),
                                    start = Offset(size.width / 2, 0f),
                                    end = Offset(size.width / 2, -lineLength),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            if (idx < lineStations.size - 1) {
                                drawLine(
                                    color = colorHex.copy(alpha = 0.5f),
                                    start = Offset(size.width / 2, size.height),
                                    end = Offset(size.width / 2, size.height + lineLength),
                                    strokeWidth = 3.dp.toPx()
                                )
                            }
                            // Station dot
                            drawCircle(
                                color = if (station.isInterchange) Color.White else colorHex,
                                radius = if (station.isInterchange) 6.dp.toPx() else 5.dp.toPx()
                            )
                            if (station.isInterchange) {
                                drawCircle(
                                    color = colorHex,
                                    radius = 4.dp.toPx(),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val stationDisp = when (lang) {
                            AppLanguage.TELUGU -> station.nameTe
                            AppLanguage.HINDI -> station.nameHi
                            else -> station.name
                        }
                        Text(
                            text = stationDisp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (station.isInterchange) {
                            Text(
                                text = "Interchange Junction",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Simulated schedule indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGreenBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (idx % 2 == 0) "Every 3m" else "Every 5m",
                            fontSize = 10.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- SCREEN 3: TICKETS & PASSES LIST ----------------------
@Composable
fun MyTicketsScreen(
    tickets: List<BookedTicket>,
    lang: AppLanguage,
    onViewQr: (BookedTicket) -> Unit,
    onStartTrip: (BookedTicket) -> Unit
) {
    if (tickets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = "No Tickets",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Booked Tickets Yet",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Purchase a pass or single journey ticket to begin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(tickets) { ticket ->
            val isPass = ticket.ticketType == "PASS"
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge type
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPass) AccentOrangeBg else AccentIndigoBg
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isPass) Translations.getString("smart_pass", lang)
                                else if (ticket.ticketType == "RETURN") Translations.getString("return_journey", lang)
                                else Translations.getString("single_journey", lang),
                                fontSize = 10.sp,
                                color = if (isPass) AccentOrange else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Ticket Status Badge
                        val statusLabel = when (ticket.status) {
                            "ACTIVE" -> Translations.getString("active", lang)
                            "USED" -> Translations.getString("used", lang)
                            else -> Translations.getString("expired", lang)
                        }
                        val statusColor = when (ticket.status) {
                            "ACTIVE" -> AccentGreen
                            "USED" -> Color(0xFF64748B)
                            else -> Color(0xFFEF4444)
                        }
                        val statusBg = when (ticket.status) {
                            "ACTIVE" -> AccentGreenBg
                            "USED" -> Color(0xFFF1F5F9)
                            else -> Color(0xFFFEF2F2)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(statusBg)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                fontSize = 10.sp,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Route details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("FROM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text(ticket.sourceStation, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        Icon(
                            imageVector = Icons.Default.TrendingFlat,
                            contentDescription = "To",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TO", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text(ticket.destStation, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("FARE PAID", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("₹${String.format("%.2f", ticket.fare)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onViewQr(ticket) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("view_qr_${ticket.id}"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Translations.getString("view_qr", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (ticket.status == "ACTIVE") {
                                Button(
                                    onClick = { onStartTrip(ticket) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("track_trip_${ticket.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ride", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- SCREEN 4: USER PROFILE ----------------------
@Composable
fun ProfileScreen(
    viewModel: MetroViewModel,
    profile: UserProfile?,
    lang: AppLanguage,
    onAddMoneyClick: () -> Unit
) {
    var nameState by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var phoneState by remember(profile) { mutableStateOf(profile?.phoneNumber ?: "") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Meta Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile Avatar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile?.name ?: "Guest Rider",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = profile?.phoneNumber ?: "No phone registered",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Card Balance Wallet View
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = Translations.getString("wallet_balance", lang),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${String.format("%.2f", profile?.walletBalance ?: 0.0)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Button(
                        onClick = onAddMoneyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.AddCard, contentDescription = "Top Up")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(Translations.getString("add_money", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Edit Profile Details Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(28.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Edit Rider Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = { Text(Translations.getString("enter_name", lang), fontWeight = FontWeight.Medium) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phoneState,
                        onValueChange = { phoneState = it },
                        label = { Text(Translations.getString("enter_phone", lang), fontWeight = FontWeight.Medium) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_phone_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.updateProfileInfo(nameState, phoneState) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(Translations.getString("save_profile", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------- SCREEN 5: AI CHATBOT & VOICE ASSISTANT ----------------------
@Composable
fun AiAssistantScreen(
    viewModel: MetroViewModel,
    lang: AppLanguage
) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }

    val quickChips = listOf(
        "How much fare from Ameerpet to Miyapur?",
        "Easiest route from HITEC City to Secunderabad?",
        "Charminar nearby metro and ticket price",
        "Book Ameerpet to Miyapur"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chat History List
        Box(modifier = Modifier.weight(1f)) {
            if (chatHistory.isEmpty()) {
                // Friendly Greeting Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Smart AI",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hyderabad Metro AI Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "I speak English, Telugu, and Hindi! Ask me fares, routes, or book directly using voice commands.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Chips Suggestions
                    Text(
                        text = "Tap to try:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    quickChips.forEach { chip ->
                        Card(
                            onClick = { viewModel.sendChatMessage(chip) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = chip,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatHistory) { (userMsg, botMsg) ->
                        // User Speech/Text Bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(14.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Text(
                                    text = userMsg,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Bot Response Bubble
                        if (botMsg.isNotEmpty() || isAiLoading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
                                        )
                                        .padding(14.dp)
                                        .widthIn(max = 280.dp)
                                ) {
                                    if (botMsg.isEmpty() && isAiLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = botMsg,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Controls (Text Field + Simulated Mic)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text(Translations.getString("chatbot_placeholder", lang), fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                trailingIcon = {
                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                viewModel.sendChatMessage(textInput)
                                textInput = ""
                            },
                            modifier = Modifier.testTag("send_chat_button")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Animated Simulated Voice Microphone Button
            Box(contentAlignment = Alignment.Center) {
                // Pulse waves while listening
                if (isListening) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f * scale))
                    )
                }

                IconButton(
                    onClick = { viewModel.triggerVoiceListening() },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Color(0xFFC62828)
                            else MaterialTheme.colorScheme.primary
                        )
                        .testTag("voice_assistant_mic")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicNone else Icons.Default.Mic,
                        contentDescription = "Voice Assistant",
                        tint = Color.White
                    )
                }
            }
        }

        // Floating Speak State Indicator
        if (isListening) {
            Text(
                text = Translations.getString("voice_listening", lang),
                color = Color(0xFFC62828),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------- SUB COMPONENT: WALLET DIALOG ----------------------
@Composable
fun WalletTopUpDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("200") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = Translations.getString("add_money", lang),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)", fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wallet_topup_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Fast selections chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("100", "200", "500").forEach { preset ->
                        OutlinedButton(
                            onClick = { amountText = preset },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("₹$preset", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(Translations.getString("cancel", lang), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 100.0
                            onConfirm(amt)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("wallet_topup_confirm"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Cash", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------------- SUB COMPONENT: QR CODE TICKET DIALOG ----------------------
@Composable
fun QrTicketDialog(
    ticket: BookedTicket,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Metro Digital QR Pass",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Translations.getString("scan_entry", lang),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Custom Canvas-drawn QR Code block!
                QrCodeView(
                    data = ticket.qrCodeData,
                    modifier = Modifier.testTag("qr_block_view")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FROM", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("TO", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ticket.sourceStation, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Text(ticket.destStation, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GATE PASS", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("FARE", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ticket.qrCodeData, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                            Text("₹${String.format("%.2f", ticket.fare)}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFEF4444))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Simulated QR code drawing helper Composable
@Composable
fun QrCodeView(data: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(180.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val size = this.size
            val gridCount = 21
            val cellW = size.width / gridCount
            val cellH = size.height / gridCount

            // Pseudo-random deterministic noise based on data hash
            val hash = data.hashCode()
            for (r in 0 until gridCount) {
                for (c in 0 until gridCount) {
                    // Position detection patterns at corners (Top Left, Top Right, Bottom Left)
                    val isCorner = (r < 6 && c < 6) || (r < 6 && c >= gridCount - 6) || (r >= gridCount - 6 && c < 6)
                    val isInnerCorner = (r in 1..4 && c in 1..4) || (r in 1..4 && c in (gridCount - 5)..(gridCount - 2)) || (r in (gridCount - 5)..(gridCount - 2) && c in 1..4)
                    val isCornerCenter = (r in 2..3 && c in 2..3) || (r in 2..3 && c in (gridCount - 4)..(gridCount - 3)) || (r in (gridCount - 4)..(gridCount - 3) && c in 2..3)

                    val isBlack = if (isCorner) {
                        if (isInnerCorner) {
                            isCornerCenter
                        } else {
                            true
                        }
                    } else {
                        // Deterministic noise
                        val pseudoValue = (r * 17 + c * 31 + hash) % 7
                        pseudoValue == 0 || pseudoValue == 2 || pseudoValue == 5
                    }

                    if (isBlack) {
                        drawRect(
                            color = Color(0xFF111111),
                            topLeft = Offset(c * cellW, r * cellH),
                            size = Size(cellW + 0.5f, cellH + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- ACTIVE TRIP simulator overlay dialog ----------------------
@Composable
fun ActiveTripSimulatorDialog(
    viewModel: MetroViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val ticket by viewModel.activeTripTicket.collectAsStateWithLifecycle()
    val stationsList by viewModel.tripStations.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentTripStationIndex.collectAsStateWithLifecycle()
    val distanceLeft by viewModel.tripDistanceLeft.collectAsStateWithLifecycle()
    val timeLeft by viewModel.tripTimeLeft.collectAsStateWithLifecycle()
    val tripAnnouncement by viewModel.tripAnnouncement.collectAsStateWithLifecycle()

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Trip Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Train,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live GPS Simulation",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.stopTripSimulation()
                            onDismiss()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Simulated Distance Announcement Box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Translations.getString("distance_destination", lang)
                                .format(distanceLeft.toInt().coerceAtLeast(1)),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Translations.getString("time_destination", lang)
                                .format(timeLeft.coerceAtLeast(2)),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrolling stops HUD view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        stationsList.forEachIndexed { sIdx, station ->
                            val isPassed = sIdx < currentIndex
                            val isCurrent = sIdx == currentIndex

                            val dispName = when (lang) {
                                AppLanguage.TELUGU -> station.nameTe
                                AppLanguage.HINDI -> station.nameHi
                                else -> station.name
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) Color(0xFFE53935)
                                            else if (isPassed) Color.Gray.copy(alpha = 0.5f)
                                            else Color(0xFF1E88E5)
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = dispName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                                    color = if (isCurrent) Color(0xFFE53935)
                                    else if (isPassed) Color.Gray
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFEBEE))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("Here", fontSize = 8.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice / Speaker Announcer Output
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Voice Announcement",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tripAnnouncement.ifEmpty { "Welcome to Hyderabad Metro Rail." },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.stopTripSimulation()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Stop Simulated Trip")
                }
            }
        }
    }
}

// ---------------------- SUB COMPONENT: ROUTE PATH PREVIEW DIALOG ----------------------
@Composable
fun RoutePreviewDialog(
    viewModel: MetroViewModel,
    lang: AppLanguage,
    onDismiss: () -> Unit
) {
    val source by viewModel.sourceStation.collectAsStateWithLifecycle()
    val dest by viewModel.destStation.collectAsStateWithLifecycle()

    val route = remember(source, dest) { MetroNetwork.findRoute(source, dest) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interactive Route Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (route != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Distance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${String.format("%.1f", route.distanceKm)} km", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Duration", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${route.durationMin} mins", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Stops", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${route.stations.size} stops", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic path finder steps scroll
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            route.stations.forEachIndexed { index, station ->
                                val colorHex = when (station.line) {
                                    MetroLine.RED -> Color(0xFFE53935)
                                    MetroLine.BLUE -> Color(0xFF1E88E5)
                                    else -> Color(0xFF4CAF50)
                                }
                                val dispName = when (lang) {
                                    AppLanguage.TELUGU -> station.nameTe
                                    AppLanguage.HINDI -> station.nameHi
                                    else -> station.name
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colorHex)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = dispName, fontSize = 12.sp)
                                    if (station.isInterchange) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE0F7FA))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("TRANSFER", fontSize = 7.sp, color = Color(0xFF006064), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Text guidance instruction
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = Translations.getString("how_to_travel", lang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val instructions = when (lang) {
                                AppLanguage.TELUGU -> route.instructionsTe
                                AppLanguage.HINDI -> route.instructionsHi
                                else -> route.instructionsEn
                            }
                            Text(
                                text = instructions,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// Inline extension to allow fullWidth modifiers
private fun Modifier.fillOuterWidth() = this.fillMaxWidth()
