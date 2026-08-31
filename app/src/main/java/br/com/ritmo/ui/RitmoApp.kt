package br.com.ritmo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.ritmo.PedometerUiState
import br.com.ritmo.RunnerProfile
import br.com.ritmo.data.DailyActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Navy = Color(0xFF10223E)
private val Blue = Color(0xFF3567EA)
private val Mint = Color(0xFF35D6B3)
private val Surface = Color(0xFFF5F7FC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RitmoApp(state: PedometerUiState, week: List<DailyActivity>, onProfile: (Int, Int) -> Unit) {
    var showSettings by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(primary = Blue, surface = Surface)) {
        Scaffold(
            containerColor = Surface,
            topBar = {
                TopAppBar(
                    title = { Text("Ritmo", fontWeight = FontWeight.Bold, color = Navy) },
                    actions = { Icon(Icons.Outlined.Settings, "Configurar perfil", Modifier.padding(16.dp).clickable { showSettings = true }, tint = Navy) }
                )
            }
        ) { padding ->
            Dashboard(state, week, Modifier.padding(padding))
        }
        if (showSettings) ProfileDialog(state.profile, { stride, weight -> onProfile(stride, weight); showSettings = false }, { showSettings = false })
    }
}

@Composable
private fun Dashboard(state: PedometerUiState, week: List<DailyActivity>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Seu movimento hoje", color = Navy, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(if (state.sensorAvailable) "Acompanhe seu ritmo em tempo real" else "Este aparelho não possui sensor de passos", color = Color(0xFF60708A))
        StepCard(state)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("DISTÂNCIA", String.format(Locale.US, "%.2f km", state.distanceKm), Modifier.weight(1f))
            MetricCard("CALORIAS", "${state.calories.roundToInt()} kcal", Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("Evolução semanal", fontWeight = FontWeight.Bold, color = Navy, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                WeeklyChart(week)
            }
        }
        Text("As estimativas usam sua passada e peso configurados. O contador é salvo diariamente no dispositivo.", color = Color(0xFF60708A), fontSize = 12.sp)
    }
}

@Composable
private fun StepCard(state: PedometerUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(28.dp)) {
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(RoundedCornerShape(38.dp)).background(Mint.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DirectionsRun, null, tint = Mint, modifier = Modifier.size(39.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("PASSOS", color = Color(0xFFB9C8E2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("${state.steps}", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Text("Cadência  ${state.cadence} SPM", color = Mint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(17.dp)) {
            Text(label, color = Color(0xFF71819B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(value, color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeeklyChart(items: List<DailyActivity>) {
    val points = remember(items) { items.takeLast(7) }
    val max = (points.maxOfOrNull { it.steps } ?: 1).coerceAtLeast(1)
    Column {
        Canvas(Modifier.fillMaxWidth().height(112.dp)) {
            val usable = size.width / 7f
            points.forEachIndexed { index, item ->
                val height = size.height * item.steps / max
                val x = usable * index + usable / 2
                drawLine(Blue, Offset(x, size.height), Offset(x, size.height - height), strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
            }
            if (points.isEmpty()) drawLine(Color(0xFFE2E8F5), Offset(0f, size.height - 2.dp.toPx()), Offset(size.width, size.height - 2.dp.toPx()), 4.dp.toPx(), StrokeCap.Round)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            points.forEach { item ->
                val day = Instant.ofEpochMilli(item.day).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE", Locale("pt", "BR"))).uppercase()
                Text(day, fontSize = 10.sp, color = Color(0xFF71819B), modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProfileDialog(profile: RunnerProfile, save: (Int, Int) -> Unit, dismiss: () -> Unit) {
    var stride by remember { mutableStateOf(profile.strideCm.toString()) }
    var weight by remember { mutableStateOf(profile.weightKg.toString()) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Seu perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Usamos estes dados para estimar distância e gasto calórico.")
                OutlinedTextField(stride, { stride = it.filter(Char::isDigit) }, label = { Text("Passada (cm)") }, singleLine = true)
                OutlinedTextField(weight, { weight = it.filter(Char::isDigit) }, label = { Text("Peso (kg)") }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { save(stride.toIntOrNull() ?: profile.strideCm, weight.toIntOrNull() ?: profile.weightKg) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancelar") } }
    )
}
