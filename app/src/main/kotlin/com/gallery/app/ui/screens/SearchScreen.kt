package com.gallery.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var query    by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text  = "Cari",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(Modifier.height(12.dp))

            // M3 Expressive SearchBar — expanded langsung di halaman (tidak full-screen)
            SearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query          = query,
                        onQueryChange  = { query = it },
                        onSearch       = { expanded = false },
                        expanded       = expanded,
                        onExpandedChange = { expanded = it },
                        placeholder    = { Text("Cari foto, album, tanggal…") },
                    )
                },
                expanded       = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Konten saat SearchBar expanded (hasil pencarian)
                // — kosong dulu, nanti diisi hasil MediaStore
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) { /* hasil pencarian nanti di sini */ }
            }

            // Empty state saat tidak ada query
            if (!expanded && query.isEmpty()) {
                Box(
                    modifier          = Modifier.fillMaxSize(),
                    contentAlignment  = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector        = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            modifier           = Modifier.size(56.dp),
                            tint               = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text      = "Ketik sesuatu untuk mulai mencari",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
