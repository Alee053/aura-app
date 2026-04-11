package org.example.aura_app.presentation.todo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.aura_app.domain.model.Todo
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodoScreen(viewModel: TodoViewModel = koinViewModel<TodoViewModel>()) {
    val todos by viewModel.todos.collectAsState()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Todo List", style = MaterialTheme.typography.headlineMedium)
        todos.forEach { todo ->
            Text(
                text = if (todo.isCompleted) "✓ ${todo.title}" else "○ ${todo.title}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}