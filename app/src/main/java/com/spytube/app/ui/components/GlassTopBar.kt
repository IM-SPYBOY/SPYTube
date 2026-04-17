package com.spytube.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spytube.app.ui.theme.NetflixRed

// Netflix-style condensed bold font
private val BebasNeue = FontFamily(
    Font(com.spytube.app.R.font.bebas_neue, FontWeight.Normal)
)

@Composable
fun GlassTopBar(
    onSearchClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "SPYTube",
            color = NetflixRed,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = BebasNeue,
            letterSpacing = 1.5.sp
        )
    }
}
