package com.saariuslystoned.mbux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class PhoneSection(val displayName: String) {
    CLAUDE("Claude"),
    CODEX("Codex"),
    DISPATCH("Dispatch"),
}

@Composable
fun PhoneSectionPicker(
    selected: PhoneSection,
    onSelect: (PhoneSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PhoneSection.entries.forEach { section ->
            FilterChip(
                selected = section == selected,
                onClick = { onSelect(section) },
                label = { Text(section.displayName) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
