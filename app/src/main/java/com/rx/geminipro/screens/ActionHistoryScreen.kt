package com.rx.geminipro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rx.geminipro.services.ActionLogEntry
import com.rx.geminipro.services.ActionLogger
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ActionHistoryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var actions by remember { mutableStateOf<List<ActionLogEntry>>(emptyList()) }

    LaunchedEffect(Unit) {
        actions = ActionLogger.getInstance()?.getRecentActions(100) ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Action History",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Row {
                TextButton(onClick = {
                    ActionLogger.getInstance()?.clearLog()
                    actions = emptyList()
                }) {
                    Text("Clear")
                }
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (actions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No actions recorded yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(actions) { action ->
                    ActionLogCard(action)
                }
            }
        }
    }
}

@Composable
fun ActionLogCard(entry: ActionLogEntry) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val timeStr = dateFormat.format(Date(entry.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.action.replace("_", " ").replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.details.isNotEmpty()) {
                Text(
                    text = entry.details.take(200),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (entry.result.isNotEmpty()) {
                Text(
                    text = "→ ${entry.result.take(100)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
