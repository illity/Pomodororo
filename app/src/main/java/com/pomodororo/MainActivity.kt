package com.pomodororo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pomodororo.model.PomodoroCycleModel
import com.pomodororo.ui.theme.PomodororoTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pomodororo.model.PomodoroSessionModel
import com.pomodororo.model.TagModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val controller = PomodoroController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d("MAIN_DEBUG", "onCreate called")
        PomodoroController.init(applicationContext)
        val serviceIntent = Intent(this, PomodoroService::class.java)
        startForegroundService(serviceIntent)

        setContent {
            MainScreen(
                model = controller.state.collectAsState().value,
                sessions = controller.sessions.collectAsState().value,
                onStart = { controller.togglePlayPause() },
                onCancel = { controller.cancel() },
                onRestart = { controller.restart() },
                onSkip = { controller.skip() },
                controller =  controller
            )
        }
    }
}


@Composable
fun TagRow(
    tag: TagModel,
    colorOptions: List<Long>,
    onUpdate: (TagModel) -> Unit,
    onSelect: (TagModel) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(tag.tag) }
    var editColor by remember { mutableStateOf(tag.color) }
    var colorExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(
                enabled = !editing, // only clickable when not editing
                onClick = { onSelect(tag) }
            )
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color(tag.color), shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))

        if (editing) {
            TextField(
                value = editName,
                onValueChange = { editName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Edit tag", color = Color.White.copy(alpha = 0.5f)) },
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Color selector
            Box {
                Row(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(editColor), shape = CircleShape)
                        .clickable { colorExpanded = !colorExpanded }
                ) {}

                DropdownMenu(
                    expanded = colorExpanded,
                    onDismissRequest = { colorExpanded = false }
                ) {
                    colorOptions.forEach { c ->
                        DropdownMenuItem(
                            text = {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(c), shape = CircleShape)
                                )
                            },
                            onClick = {
                                editColor = c
                                colorExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (editName.isNotBlank()) {
                    onUpdate(tag.copy(tag = editName, color = editColor))
                    editing = false
                }
            }) { Text("Save") }

            Spacer(modifier = Modifier.width(4.dp))
            Button(onClick = { editing = false }) { Text("Cancel") }

        } else {
            Text(tag.tag, modifier = Modifier.weight(1f), color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                editing = true
                editName = tag.tag
                editColor = tag.color
            }) { Text("Edit") }
        }
    }
}

@Composable
fun Tag(controller: PomodoroController, tagName: String?, tagColor: Long) {
    val tags by controller.tags.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // New tag state
    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember { mutableStateOf(0xFFF3644C) }

    val colorOptions = listOf(
        0xFFF44336, 0xFF4CAF50, 0xFF2196F3, 0xFFFFC107, 0xFF9C27B0
    )

    Column {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp)
        ) {
            // Get the currently selected tag

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp)
            ) {
                Box( modifier = Modifier .size(10.dp) .background(Color(tagColor), shape = CircleShape) )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    tagName ?: "Tags", // <-- show current tag, fallback to "Tags"
                    color = MaterialTheme.colorScheme.onBackground
                )
//                val icon = if (expanded) R.drawable.play else R.drawable.pause
//                Icon(
//                    painter = painterResource(icon),
//                    contentDescription = "Expand",
//                    modifier = Modifier.size(16.dp)
//                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val icon = if (expanded) R.drawable.play else R.drawable.pause
            Icon(
                painter = painterResource(icon),
                contentDescription = "Expand",
                modifier = Modifier.size(16.dp)
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 4.dp)) {
                tags.forEach { tag ->
                    TagRow(
                        tag = tag,
                        colorOptions = colorOptions,
                        onUpdate = { updatedTag -> controller.updateTag(updatedTag) },
                        onSelect = { selectedTag ->
                            controller.selectTag(selectedTag)
                            expanded = false // <-- collapse menu
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Add new tag row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        placeholder = {
                            Text("New tag", color = Color.White.copy(alpha = 0.5f))
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    var newColorExpanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(newTagColor), shape = CircleShape)
                                .clickable { newColorExpanded = !newColorExpanded }
                        ) {}
                        DropdownMenu(
                            expanded = newColorExpanded,
                            onDismissRequest = { newColorExpanded = false }
                        ) {
                            colorOptions.forEach { c ->
                                DropdownMenuItem(
                                    text = {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(c), shape = CircleShape)
                                        )
                                    },
                                    onClick = {
                                        newTagColor = c
                                        newColorExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newTagName.isNotBlank()) {
                            controller.addTag(TagModel(tag = newTagName, color = newTagColor))
                            newTagName = ""
                            newTagColor = 0xFFF3644C
                        }
                    }) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun Timer(seconds: Int) {
    val minutes = seconds / 60
    val remaining = seconds % 60
    val thinFont = FontFamily(
        Font(R.font.lato_hairline, FontWeight.W600) // make sure this font file exists in res/font
    )

    Text(
        text = "%02d:%02d".format(minutes, remaining),
        fontSize = 96.sp,
        fontFamily = thinFont,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground
    )

}

@Composable
fun CurrentCharacter(phase: String) {
    Image(
        painter = painterResource(if (phase == "focus") R.drawable.dororo else R.drawable.shigeo),
        contentDescription = null,
        modifier = Modifier.size(256.dp),
        // O ColorFilter aplica a cor 'dynamicColor' em tempo real
//        colorFilter = ColorFilter.tint(Color.Blue)
    )
}

@Composable
fun RestartButton(onClick: () -> Unit, color: Long) {
    Icon(
        painter = painterResource(R.drawable.arrowpath),
        contentDescription = "Start",
        tint = Color(color),
        modifier = Modifier.size(36.dp)
            .clickable(onClick = onClick)
    )
}
@Composable
fun CancelButton(onClick: () -> Unit, color: Long) {
    Icon(
        painter = painterResource(R.drawable.xmark),
        contentDescription = "Start",
        tint = Color(color),
        modifier = Modifier.size(36.dp)
            .clickable(onClick = onClick)

    )
}

@Composable
fun StartButton(isRunning: Boolean, onClick: () -> Unit, color: Long) {
    val painter = painterResource(if (isRunning) R.drawable.pause else R.drawable.play)
    Icon(
        painter = painter,
        contentDescription = "Start",
        tint = Color(color),
        modifier = Modifier.size(36.dp)
            .clickable(onClick = onClick)

    )
}

@Composable
fun SkipButton(onClick: () -> Unit, color: Long) {
    val painter = painterResource(R.drawable.forward)
    Icon(
        painter = painter,
        contentDescription = "Start",
        tint = Color(color),
        modifier = Modifier.size(36.dp)
            .clickable(onClick = onClick)

    )
}

@Composable
fun TaskProgressBar(
    total: Int,
    sessions: List<PomodoroSessionModel>,
    controller: PomodoroController
) {
    // Collect current tags to get updated colors
    val tags by controller.tags.collectAsState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (s in sessions) {
            if (s.active && s.currentPhase == "focus") continue

            // Find the latest color for this session's tag
            val tagColor = tags.find { it.tag == s.tag }?.color ?: s.color

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (s.active && s.currentPhase == "rest")
                            Color(tagColor).copy(alpha = 0.5F)
                        else
                            Color(tagColor),
                        CircleShape
                    )
                    .border(1.dp, Color.Gray, shape = CircleShape)
            )
        }

        repeat(total - sessions.size + if (sessions.isNotEmpty() && sessions.last().currentPhase == "focus") 1 else 0) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .border(1.dp, Color.Gray, shape = CircleShape)
            )
        }
    }
}


//@Preview(showBackground = true, showSystemUi = false)
//@Composable
//fun Preview() {
//    val controller = PomodoroController
//    MainScreen(
//    PomodoroModel(),
//    { controller.togglePlayPause() },
//        { controller.cancel() },
//        { controller.cancel() },
//        { controller.skip()}
//    )
//}


@Composable
fun MonthlyCalendar(
    sessions: List<PomodoroSessionModel>,
    month: LocalDate,
    selectedDate: LocalDate,
    onDayClick: (LocalDate) -> Unit
) {

    val firstDayOfMonth = month.withDayOfMonth(1)
    val daysInMonth = month.lengthOfMonth()
    val firstWeekDay = firstDayOfMonth.dayOfWeek.value % 7

    Column {

        var day = 1

        for (week in 0 until 6) {

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                for (weekDay in 0 until 7) {

                    if (week == 0 && weekDay < firstWeekDay || day > daysInMonth) {
                        Box(modifier = Modifier.size(44.dp))
                    } else {

                        val date = firstDayOfMonth.plusDays((day - 1).toLong())
                        val (start, end) = date.toStartAndEndOfDayMillis()

                        val sessionsOfDay = sessions.filter {
                            it.endTime in start..end
                        }

                        DayCell(
                            date = date,
                            sessions = sessionsOfDay,
                            isSelected = date == selectedDate,
                            onClick = { onDayClick(date) }
                        )



                        day++
                    }
                }
            }

            if (day > daysInMonth) break
        }
    }
}

@Composable
fun DayCell(
    date: LocalDate,
    sessions: List<PomodoroSessionModel>,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val sessionCount = sessions.size
    val gridSize = kotlin.math.ceil(kotlin.math.sqrt(sessionCount.toDouble()))
        .toInt()
        .coerceAtLeast(1)

    val spacing = 1.dp
    val gridArea = 28.dp   // area reserved for dots
    val circleSize = (gridArea - spacing * (gridSize - 1)) / gridSize

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(44.dp)
            .height(68.dp)
            .padding(2.dp)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(2.dp)
    ) {

        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 10.sp,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                Color.Gray
        )

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {

            var index = 0

            repeat(gridSize) {

                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {

                    repeat(gridSize) {

                        val session = sessions.getOrNull(index)

                        Box(
                            modifier = Modifier
                                .size(circleSize)
                                .background(
                                    if (session != null)
                                        Color(session.color)
                                    else
                                        Color.Gray.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        )

                        index++
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun StatisticsScreen(
    controller: PomodoroController,
    onBack: () -> Unit
) {

    val tags by controller.tags.collectAsState()
    val sessionsMap = remember { mutableStateMapOf<String, List<PomodoroSessionModel>>() }

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val baseMonth = LocalDate.now()

    val pagerState = rememberPagerState(
        initialPage = 5000,
        pageCount = { 10000 }
    )

    val currentMonth = baseMonth.plusMonths((pagerState.currentPage - 5000).toLong())

    val allSessions = sessionsMap.values.flatten()

    val monthStats = remember(currentMonth, allSessions) {

        val startOfMonth = currentMonth.withDayOfMonth(1)
        val endOfMonth = currentMonth.withDayOfMonth(currentMonth.lengthOfMonth())

        val startMillis = startOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = endOfMonth.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val monthSessions = allSessions.filter {
            it.endTime in startMillis until endMillis
        }

        val totalSessions = monthSessions.size

        val sessionsByDay = monthSessions.groupBy {
            Instant.ofEpochMilli(it.endTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

        val averagePerDay =
            if (sessionsByDay.isEmpty()) 0.0
            else totalSessions.toDouble() / sessionsByDay.size

        // current streak
        var streak = 0
        var date = LocalDate.now()

        while (true) {
            val count = sessionsByDay[date]?.size ?: 0
            if (count > 0) {
                streak++
                date = date.minusDays(1)
            } else {
                break
            }
        }

        Triple(totalSessions, averagePerDay, streak)

    }

    tags.forEach { tag ->
        LaunchedEffect(tag) {
            val sessions = controller.loadSessionsByTag(tag.tag)
            sessionsMap[tag.tag] = sessions
        }
    }

    PomodororoTheme {
        BackHandler {
            onBack()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .safeDrawingPadding()
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrowuturnleft),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "${currentMonth.monthValue}/${currentMonth.year}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }



            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState
            ) { page ->

                val month = baseMonth.plusMonths((page - 5000).toLong())

                MonthlyCalendar(
                    sessions = allSessions,
                    month = month,
                    selectedDate = selectedDate,
                    onDayClick = { clickedDate ->
                        selectedDate = clickedDate
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (tags.isEmpty()) {
                Text(
                    "No tags available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {

                val (startOfDay, endOfDay) = selectedDate.toStartAndEndOfDayMillis()

                tags.forEach { tag ->

                    val allSessions = sessionsMap[tag.tag] ?: emptyList()

                    val filteredSessions = allSessions.filter {
                        it.endTime in startOfDay..endOfDay
                    }

                    if (filteredSessions.isNotEmpty()) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(tag.color), shape = CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = tag.tag,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Text(
                            text = "${filteredSessions.size} sessions completed",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 18.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                val (totalSessionsMonth, avgSessionsDay, currentStreak) = monthStats

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {

                    StatItem(
                        value = totalSessionsMonth.toString(),
                        label = "Total"
                    )

                    StatItem(
                        value = "%.1f".format(avgSessionsDay),
                        label = "Avg/day"
                    )

                    StatItem(
                        value = "$currentStreak",
                        label = "Streak"
                    )
                }
            }
        }
    }
}
/**
 * Converts a LocalDate into a pair of (startOfDayMillis, endOfDayMillis)
 */
fun LocalDate.toStartAndEndOfDayMillis(): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()

    val startOfDay = this
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

    val endOfDay = this
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli() - 1

    return startOfDay to endOfDay
}

@Composable
fun MainScreen(
    model: PomodoroCycleModel,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit,
    onSkip: () -> Unit,
    sessions: List<PomodoroSessionModel>,
    controller: PomodoroController
) {
    var showStatistics by remember { mutableStateOf(false) }

    if (showStatistics) {
        // Show the secondary screen
        StatisticsScreen(controller = controller, onBack = { showStatistics = false })
    } else {
        PomodororoTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .safeDrawingPadding(),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                // Top section: Tag, Timer, TaskProgressBar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Tag(
                        controller = controller,
                        tagName = model.tag,
                        tagColor = model.color
                    )

                    Spacer(modifier = Modifier.size(16.dp))
                    Timer(model.remainingSeconds)
                    Spacer(modifier = Modifier.size(16.dp))
                    TaskProgressBar(
                        total = model.totalSessions,
                        sessions = sessions,
                        controller = controller
                    )

                    Spacer(modifier = Modifier.size(16.dp))
                    Button(
                        onClick = { showStatistics = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(model.color),    // cor de fundo do botão
                            contentColor = Color.Black// cor do texto e ícone
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.chartbar),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Statistics")
                        }
                    }
                }

                // Bottom section: Character + controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CurrentCharacter(model.currentPhase)
                    Spacer(modifier = Modifier.size(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            24.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        StartButton(isRunning = model.isRunning, onClick = onStart, color = model.color)
                        SkipButton(onClick = onSkip, color = model.color)
                        RestartButton(onClick = onRestart, color = model.color)
                        CancelButton(onClick = onCancel, color = model.color)
                    }
                }
            }
        }
    }
}