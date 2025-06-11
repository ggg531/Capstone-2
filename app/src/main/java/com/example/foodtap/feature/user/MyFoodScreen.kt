package com.example.foodtap.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodtap.ui.theme.Main
import com.example.foodtap.util.FileManager
import kotlinx.coroutines.delay

@Composable
fun MyFoodScreen(navController: NavController, viewModel: MyViewModel = viewModel()) {
    val context = LocalContext.current
    var confirmedExp by remember { mutableStateOf(emptyList<Triple<String, String, Int>>()) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        confirmedExp = FileManager.loadConfirmedExpiration(context)

        delay(500)
        viewModel.speak("구매 식품 관리 페이지입니다.")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Main)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "구매 식품 관리",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.semantics { contentDescription = "구매 식품 관리 페이지" }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 30.dp),
                thickness = 1.dp,
                color = Color.LightGray
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "소비기한 임박 순",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val maxIndex = confirmedExp.lastIndex.coerceAtLeast(1)

                itemsIndexed(confirmedExp) { index, (productName, expiration, dDay) ->
                    val dDayStr = if (dDay >= 0) "D-$dDay" else "D+${-dDay}"
                    val displayText = if (productName.isNotBlank())
                        "$productName | $dDayStr"
                    else
                        "$expiration ($dDayStr)"

                    val fraction = index.toFloat() / maxIndex
                    val r = (214 + (255 - 214) * fraction).toInt()
                    val g = (40 + (255 - 40) * fraction).toInt()
                    val b = (40 + (255 - 40) * fraction).toInt()
                    val buttonColor = Color(r, g, b)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.stopSpeaking()
                                selectedIndex = index
                                showDialog = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .size(width = 360.dp, height = 80.dp)
                        ) {
                            Text(
                                text = displayText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                modifier = Modifier.semantics { contentDescription = displayText }
                            )
                        }
                    }
                }
            }
        }

        if (showDialog && selectedIndex != -1) {
            val target = confirmedExp[selectedIndex]

            LaunchedEffect(showDialog, selectedIndex) {
                val productName = target.first
                val expiration = target.second
                val dDay = target.third
                val dDayStr = if (dDay >= 0) "$dDay 일" else "${-dDay} 일"
                val speakText = buildString {
                    if (productName.isNotBlank()) append("식품명 $productName")
                    append("소비기한이 $expiration 까지로, $dDayStr 남은 제품을 삭제하시겠습니까?")
                }
                viewModel.speak(speakText)
            }

            AlertDialog(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "구매 식품 삭제",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)) {
                        if (target.first.isNotBlank()) {
                            Text(
                                text = "식품명",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = target.first,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }

                        val dDayStr = if (target.third >= 0) "D-${target.third}" else "D+${-target.third}"
                        Text(
                            text = "소비기한",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "${target.second} ($dDayStr)",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.Black,
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            FileManager.deleteConfirmedExpiration(context, target)
                            confirmedExp = confirmedExp.toMutableList().also {
                                it.removeAt(selectedIndex)
                            }
                            showDialog = false
                            selectedIndex = -1
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors =  ButtonDefaults.buttonColors(containerColor = Color(0xFFD62828)),
                        modifier = Modifier.size(width = 360.dp, height = 80.dp)
                    ) {
                        Text(
                            text = "삭제",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showDialog = false
                            selectedIndex = -1
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors =  ButtonDefaults.buttonColors(containerColor = Main),
                        modifier = Modifier.size(width = 360.dp, height = 80.dp)
                    ) {
                        Text(
                            text = "취소",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                },
                onDismissRequest = {}
            )
        }

        /*
                Button(
                    onClick = {
                        viewModel.stopSpeaking()
                        navController.navigate("camera")
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Show),
                    modifier = Modifier
                        .padding(bottom = 80.dp)
                        .size(width = 330.dp, height = 110.dp)
                        .border(3.dp, Color.Black, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "촬영 페이지",
                        color = Color.Black,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { contentDescription = "촬영 페이지로 이동" }
                    )
                }

         */
    }
}