/*
 * This file is part of Tuner.
 * Copyright (C) 2026 Philipp Bobek <philipp.bobek@mailbox.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bobek.tuner.ui.tunings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.bobek.tuner.R
import com.bobek.tuner.domain.TUNINGS
import com.bobek.tuner.domain.Tuning
import com.bobek.tuner.ui.tuner.ComposeTunerViewModel
import com.bobek.tuner.ui.tuner.ITunerViewModel

/** Lists the chromatic tuner and every instrument tuning, grouped by instrument; picking one returns to the tuner. */
@Composable
@PreviewScreenSizes
@OptIn(ExperimentalMaterial3Api::class)
fun TuningsScreen(
    viewModel: ITunerViewModel = ComposeTunerViewModel(),
    onBackClick: () -> Unit = {}
) {
    val selected by viewModel.getTuningFlow().collectAsState()
    val solfegeNotation by viewModel.getSolfegeNotationFlow().collectAsState()

    val select: (Tuning?) -> Unit = { tuning ->
        viewModel.setTuning(tuning)
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tunings)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp
            )
        ) {
            item(key = "chromatic") {
                TuningRow(
                    title = stringResource(R.string.tuning_chromatic),
                    notes = stringResource(R.string.tuning_chromatic_summary),
                    selected = selected == null,
                    onClick = { select(null) }
                )
            }
            TUNINGS.groupBy { it.instrument }.forEach { (instrument, tunings) ->
                item(key = "header_${instrument.name}") {
                    HorizontalDivider()
                    Text(
                        text = instrument.label(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                    )
                }
                items(tunings, key = { it.id }) { tuning ->
                    TuningRow(
                        title = tuning.variantLabel(),
                        notes = tuning.notesLabel(solfegeNotation),
                        selected = tuning.id == selected?.id,
                        onClick = { select(tuning) },
                        monospaceNotes = true
                    )
                }
            }
        }
    }
}

@Composable
private fun TuningRow(
    title: String,
    notes: String,
    selected: Boolean,
    onClick: () -> Unit,
    monospaceNotes: Boolean = false
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(
                text = notes,
                fontFamily = if (monospaceNotes) FontFamily.Monospace else null
            )
        },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    )
}
