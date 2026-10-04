package com.pomodororo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.pomodororo.model.PomodoroSessionModel
import com.pomodororo.model.TagModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

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
fun ColorPickerDialog(
    initialColor: Long,
    onColorSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember {
        mutableLongStateOf(initialColor)
    }

    val controller = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pick a color")
        },
        text = {
            Column {

                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    controller = controller,
                    onColorChanged = { envelope ->
                        selectedColor =
                            envelope.color
                                .toArgb()
                                .toLong()
                                .and(0xFFFFFFFFL)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                AlphaSlider(
                    modifier = Modifier.fillMaxWidth(),
                    controller = controller
                )

                Spacer(modifier = Modifier.height(8.dp))

                BrightnessSlider(
                    modifier = Modifier.fillMaxWidth(),
                    controller = controller
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            Color(selectedColor.toInt()),
                            CircleShape
                        )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onColorSelected(selectedColor)
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun ColorPickerButton(
    color: Long,
    size: Dp = 24.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Color(color.toInt()),
                CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun Tag(
    controller: PomodoroController,
    tagName: String?,
    tagColor: Long
) {
    val tags by controller.tags.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    var newTagName by remember { mutableStateOf("") }
    var newTagColor by remember {
        mutableLongStateOf(0xFFF3644CL)
    }

    var showColorPicker by remember {
        mutableStateOf(false)
    }

    Column {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        Color(tagColor.toInt()),
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = tagName ?: "Tags",
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.play
                    else R.drawable.pause
                ),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        if (expanded) {

            Column(
                modifier = Modifier.padding(
                    start = 16.dp,
                    top = 4.dp
                )
            ) {

                tags.forEach { tag ->

                    TagRow(
                        tag = tag,
                        onUpdate = controller::updateTag,
                        onSelect = {
                            controller.selectTag(it)
                            expanded = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    TextField(
                        value = newTagName,
                        onValueChange = {
                            newTagName = it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("New tag")
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    ColorPickerButton(
                        color = newTagColor,
                        size = 32.dp,
                        onClick = {
                            showColorPicker = true
                        }
                    )

                    if (showColorPicker) {
                        ColorPickerDialog(
                            initialColor = newTagColor,
                            onColorSelected = {
                                newTagColor = it
                            },
                            onDismiss = {
                                showColorPicker = false
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (newTagName.isNotBlank()) {

                                controller.addTag(
                                    TagModel(
                                        tag = newTagName,
                                        color = newTagColor
                                    )
                                )

                                newTagName = ""
                                newTagColor = 0xFFF3644CL
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun TagRow(
    tag: TagModel,
    onUpdate: (TagModel) -> Unit,
    onSelect: (TagModel) -> Unit
) {
    var editing by remember { mutableStateOf(false) }

    var editName by remember {
        mutableStateOf(tag.tag)
    }

    var editColor by remember {
        mutableLongStateOf(tag.color)
    }

    var showColorPicker by remember {
        mutableStateOf(false)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(
                enabled = !editing
            ) {
                onSelect(tag)
            }
    ) {

        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    Color(tag.color.toInt()),
                    CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (editing) {

            TextField(
                value = editName,
                onValueChange = {
                    editName = it
                },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Edit tag")
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            ColorPickerButton(
                color = editColor,
                onClick = {
                    showColorPicker = true
                }
            )

            if (showColorPicker) {
                ColorPickerDialog(
                    initialColor = editColor,
                    onColorSelected = {
                        editColor = it
                    },
                    onDismiss = {
                        showColorPicker = false
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (editName.isNotBlank()) {

                        onUpdate(
                            tag.copy(
                                tag = editName,
                                color = editColor
                            )
                        )

                        editing = false
                    }
                }
            ) {
                Text("Save")
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = {
                    editing = false
                    editName = tag.tag
                    editColor = tag.color
                }
            ) {
                Text("Cancel")
            }

        } else {

            Text(
                text = tag.tag,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    editing = true
                    editName = tag.tag
                    editColor = tag.color
                }
            ) {
                Text("Edit")
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
fun StartButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    color: Long
) {
    val painter = painterResource(
        if (isRunning) R.drawable.pause
        else R.drawable.play
    )

    Icon(
        painter = painter,
        contentDescription = "Start",
        tint = Color(color),
        modifier = Modifier
            .size(36.dp)
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
fun DayTimelineSingleLine(
    sessions: List<PomodoroSessionModel>,
    date: LocalDate,
    modifier: Modifier = Modifier,
    sessionDurationSeconds: Long = 1500L
) {
    val zone = ZoneId.systemDefault()

    val startOfDay = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    val strokeWidth = 12.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth)
    ) {
        val widthPx = size.width
        val centerY = size.height / 2f

        val millisInDay = 24f * 60f * 60f * 1000f
        val strokePx = size.height

        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, centerY),
            end = Offset(widthPx, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Butt
        )

        sessions.forEach { session ->

            val sessionEnd = session.endTime
            val sessionStart = sessionEnd - (sessionDurationSeconds * 1000)

            val clampedStart = maxOf(sessionStart, startOfDay)
            val clampedEnd = minOf(sessionEnd, endOfDay)

            if (clampedEnd <= startOfDay || clampedStart >= endOfDay) return@forEach

            val startOffset = (clampedStart - startOfDay).toFloat()
            val endOffset = (clampedEnd - startOfDay).toFloat()

            val startX = (startOffset / millisInDay) * widthPx
            val endX = (endOffset / millisInDay) * widthPx

            drawLine(
                color = Color(session.color),
                start = Offset(startX, centerY),
                end = Offset(endX, centerY),
                strokeWidth = strokePx,
                cap = StrokeCap.Butt
            )
        }
    }
}

@Composable
fun MonthTimeline(
    sessions: List<PomodoroSessionModel>,
    month: LocalDate,
    modifier: Modifier = Modifier
) {
    val daysInMonth = month.lengthOfMonth()
    val textColor = MaterialTheme.colorScheme.onBackground

    Column(modifier = modifier) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(32.dp))

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00", fontSize = 10.sp, color = textColor)
                Text("06", fontSize = 10.sp, color = textColor)
                Text("12", fontSize = 10.sp, color = textColor)
                Text("18", fontSize = 10.sp, color = textColor)
                Text("24", fontSize = 10.sp, color = textColor)
            }
        }

        for (day in 1..daysInMonth) {
            val date = month.withDayOfMonth(day)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            ) {
                DayTimelineSingleLine(
                    sessions = sessions,
                    date = date,
                    modifier = Modifier.matchParentSize()
                )

                Text(
                    text = day.toString().padStart(2, '0'),
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    color = textColor,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                )
            }
        }
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
//
//            DayTimelineSingleLine(
//                sessions = allSessions,
//                date = selectedDate,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 12.dp)
//            )

            if (tags.isEmpty()) {
                Text(
                    "No tags available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else {

                val (startOfDay, endOfDay) = selectedDate.toStartAndEndOfDayMillis()

                tags.forEach { tag ->

                    val allSessions = sessionsMap[tag.tag] ?: emptyList()

                    val filteredSessions = allSessions.filter {
                        it.endTime in startOfDay..endOfDay
                    }

                    if (filteredSessions.isNotEmpty()) {

//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            modifier = Modifier.padding(vertical = 4.dp)
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .size(10.dp)
//                                    .background(Color(tag.color), shape = CircleShape)
//                            )
//
//                            Spacer(modifier = Modifier.width(8.dp))
//
//                            Text(
//                                text = tag.tag,
//                                color = MaterialTheme.colorScheme.onBackground
//                            )
//                        }

//                        Text(
//                            text = "${filteredSessions.size} sessions completed",
//                            color = MaterialTheme.colorScheme.onSurfaceVariant,
//                            modifier = Modifier.padding(start = 18.dp)
//                        )

//                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                val (totalSessionsMonth, avgSessionsDay, currentStreak) = monthStats

//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 12.dp),
//                    horizontalArrangement = Arrangement.SpaceEvenly
//                ) {
//
//                    StatItem(
//                        value = totalSessionsMonth.toString(),
//                        label = "Total"
//                    )
//
//                    StatItem(
//                        value = "%.1f".format(avgSessionsDay),
//                        label = "Avg/day"
//                    )
//
//                    StatItem(
//                        value = "$currentStreak",
//                        label = "Streak"
//                    )
//                }
            }

            MonthTimeline(
                sessions = allSessions,
                month = selectedDate
            )
            TagConsolidationChart(
                sessions = allSessions,
                month = selectedDate
            )
        }
    }
}

@Composable
fun TagConsolidationChart(
    sessions: List<PomodoroSessionModel>,
    month: LocalDate,
    modifier: Modifier = Modifier,
    sessionDurationMinutes: Int = 25
) {

    val startOfMonth = month.withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val endOfMonth = month.plusMonths(1)
        .withDayOfMonth(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val monthSessions = sessions.filter {
        it.endTime in startOfMonth until endOfMonth
    }

    val grouped = monthSessions.groupBy { it.tag }

    val chartData = grouped.map { (tag, tagSessions) ->

        val totalMinutes = tagSessions.size * sessionDurationMinutes
        val totalHours = totalMinutes / 60f

        Triple(
            tag ?: "No Tag",
            totalHours,
            tagSessions.firstOrNull()?.color ?: 0xFFFFFFFF
        )
    }
        .sortedByDescending { it.second }

    val maxHours = chartData.maxOfOrNull { it.second } ?: 1f

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
//
//        Text(
//            text = "Hours by Tag",
//            fontSize = 18.sp,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onBackground
//        )

        Spacer(modifier = Modifier.height(16.dp))

        chartData.forEach { (tagName, hours, color) ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(color), CircleShape)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = tagName,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "%.1fh".format(hours),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hours / maxHours)
                            .height(18.dp)
                            .background(
                                Color(color),
                                shape = CircleShape
                            )
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
fun SessionCard(
    session: PomodoroSessionModel
) {
    val zone = ZoneId.systemDefault()

    val end =
        Instant.ofEpochMilli(session.endTime)
            .atZone(zone)
            .toLocalDateTime()

    val start = end.minusMinutes(25)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    Modifier
                        .size(12.dp)
                        .background(
                            Color(session.color),
                            CircleShape
                        )
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = session.tag ?: "Sem tag",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            DayTimelineSingleLine(
                sessions = listOf(session),
                date = end.toLocalDate(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Row {

                Text(
                    "%02d:%02d".format(
                        start.hour,
                        start.minute
                    )
                )

                Text("  →  ")

                Text(
                    "%02d:%02d".format(
                        end.hour,
                        end.minute
                    )
                )

                Spacer(Modifier.weight(1f))

                Text("25 min")
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "#${session.id}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
@Composable
fun SessionHistoryDrawer(
    controller: PomodoroController
) {
    val tags by controller.tags.collectAsState()

    val sessionsMap = remember {
        mutableStateMapOf<String, List<PomodoroSessionModel>>()
    }

    tags.forEach { tag ->
        LaunchedEffect(tag) {
            sessionsMap[tag.tag] =
                controller.loadSessionsByTag(tag.tag)
        }
    }

    val allSessions = sessionsMap.values.flatten()

    val sessions = allSessions;

    var selectedDate by remember {
        mutableStateOf(LocalDate.now())
    }

    val pagerState = rememberPagerState(
        initialPage = 5000,
        pageCount = { 10000 }
    )

    val baseMonth = LocalDate.now()

    val currentMonth =
        baseMonth.plusMonths((pagerState.currentPage - 5000).toLong())
    Text(
        "${currentMonth.monthValue}/${currentMonth.year}"
    )

    HorizontalPager(
        state = pagerState
    ) { page ->

        MonthlyCalendar(
            sessions = sessions,
            month = baseMonth.plusMonths((page - 5000).toLong()),
            selectedDate = selectedDate,
            onDayClick = {
                selectedDate = it
            }
        )
    }
    val (start, end) = selectedDate.toStartAndEndOfDayMillis()

    val daySessions = sessions
        .filter {
            it.endTime in start..end
        }
        .sortedBy { it.endTime }
    LazyColumn {

        items(daySessions) { session ->

            SessionCard(session)

        }

    }
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
        val context = LocalContext.current
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        val scope = rememberCoroutineScope()
        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { uri ->

            if (uri != null) {
                scope.launch {
                    exportSessions(
                        context = context,
                        controller = controller,
                        uri = uri
                    )
                }
            }
        }
        PomodororoTheme {

            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        SessionHistoryDrawer(controller = controller)
                    }
                }
            ) {
                var menuExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .safeDrawingPadding()
                ) {

                    // Menu hamburguer
                    IconButton(
                        onClick = { menuExpanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {

                        DropdownMenuItem(
                            text = { Text("Histórico") },
                            onClick = {
                                menuExpanded = false
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Exportar CSV") },
                            onClick = {
                                menuExpanded = false
                                exportLauncher.launch("pomodororo_sessions.csv")
                            }
                        )
                    }

                    // Parte superior
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Tag(
                            controller = controller,
                            tagName = model.tag,
                            tagColor = model.color
                        )

                        Spacer(Modifier.height(16.dp))

                        Timer(model.remainingSeconds)

                        Spacer(Modifier.height(16.dp))

                        TaskProgressBar(
                            total = model.totalSessions,
                            sessions = sessions,
                            controller = controller
                        )

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = { showStatistics = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(model.color),
                                contentColor = Color.Black
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                Icon(
                                    painter = painterResource(R.drawable.chartbar),
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(8.dp))

                                Text("View Statistics")
                            }
                        }
                    }

                    // Parte inferior
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        CurrentCharacter(model.currentPhase)

                        Spacer(Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {

                            StartButton(
                                isRunning = model.isRunning,
                                onClick = onStart,
                                color = model.color
                            )

                            SkipButton(
                                onClick = onSkip,
                                color = model.color
                            )

                            RestartButton(
                                onClick = onRestart,
                                color = model.color
                            )

                            CancelButton(
                                onClick = onCancel,
                                color = model.color
                            )
                        }
                    }
                }
            }
        }
    }
}

fun sessionsToCsv(
    sessions: List<PomodoroSessionModel>
): String {

    return buildString {

        appendLine(
            "id,tag,color,phase,start,end,duration,active"
        )

        sessions.forEach {

            appendLine(
                listOf(
                    it.id,
                    it.tag,
                    it.color,
                    it.currentPhase,
                    it.endTime-1500,
                    it.endTime,
                    1500, //duração
                    it.active
                ).joinToString(",")
            )

        }

    }

}

suspend fun exportSessions(
    context: Context,
    controller: PomodoroController,
    uri: Uri
) = withContext(Dispatchers.IO) {

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    val tags = controller.tags.first()

    val sessions = buildList {
        tags.forEach { tag ->
            addAll(controller.loadSessionsByTag(tag.tag))
        }
    }.sortedBy { it.endTime }

    val csv = buildString {

        appendLine(
            "id,tag,color,phase,start,end,duration,active"
        )

        sessions.forEach { session ->

            val start = Instant.ofEpochMilli(session.endTime-1500)
                .atZone(ZoneId.systemDefault())
                .format(formatter)

            val end = Instant.ofEpochMilli(session.endTime)
                .atZone(ZoneId.systemDefault())
                .format(formatter)

            appendLine(
                listOf(
                    session.id,
                    session.tag,
                    session.color,
                    session.currentPhase,
                    start,
                    end,
                    1500,
                    session.active
                ).joinToString(",")
            )
        }
    }

    context.contentResolver.openOutputStream(uri)?.use {
        it.write(csv.toByteArray(Charsets.UTF_8))
        it.flush()
    }
}