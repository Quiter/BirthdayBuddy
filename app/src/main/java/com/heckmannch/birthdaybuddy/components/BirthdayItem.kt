package com.heckmannch.birthdaybuddy.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heckmannch.birthdaybuddy.model.BirthdayContact
import com.heckmannch.birthdaybuddy.utils.formatBirthdayGerman

@Composable
fun BirthdayItem(contact: BirthdayContact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically // Zentriert beide Spalten schön mittig
        ) {
            // Linke Spalte (bekommt das "weight", damit sie sich anpasst)
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 2, // Erlaubt maximal 2 Zeilen für sehr lange Namen
                    overflow = TextOverflow.Ellipsis // Macht "..." am Ende, wenn es nicht passt
                )
                Text(text = "Datum: ${formatBirthdayGerman(contact.birthday)}", fontSize = 14.sp)
            }

            // Rechte Spalte (ohne weight, nimmt exakt den Platz, den sie braucht)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Wird ${contact.age}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "in ${contact.remainingDays} Tagen")
            }
        }
    }
}