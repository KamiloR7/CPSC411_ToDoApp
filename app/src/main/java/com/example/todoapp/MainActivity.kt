package com.example.todoapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

// Simple data class to hold each to-do item
data class ToDoItem(
    val id: Int,  // Unique ID for each item
    val text: String,  // The task text
    val isCompleted: Boolean = false  // If the task is done
)

// Saver to remember the item across app restarts or rotations
val ToDoItemSaver = Saver<ToDoItem, Triple<Int, String, Boolean>>(
    save = { Triple(it.id, it.text, it.isCompleted) },  // Save as three parts
    restore = { ToDoItem(it.first, it.second, it.third) }  // Restore from three parts
)

// Saver for the list of items
val todoListSaver = listSaver<SnapshotStateList<ToDoItem>, ToDoItem>(
    save = { it.toList() },  // Convert list to regular list for saving
    restore = { mutableStateListOf<ToDoItem>().apply { addAll(it) } }  // Create mutable list and add items
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoApp()  // Start the app UI
        }
    }
}

@Composable
fun TodoApp() {
    val context = LocalContext.current  // For showing toast messages

    // Lists to hold active and completed tasks, saved across config changes
    val activeItems = rememberSaveable(saver = todoListSaver) {
        mutableStateListOf<ToDoItem>()  // Empty list at start
    }
    val completedItems = rememberSaveable(saver = todoListSaver) {
        mutableStateListOf<ToDoItem>()  // Empty list at start
    }

    // Text input for new tasks
    var inputText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))  // Start empty
    }

    // Flag to show error if input is empty
    var showError by remember { mutableStateOf(false) }

    // Function to add a new task
    fun addItem() {
        val text = inputText.text.trim()  // Remove extra spaces
        if (text.isNotEmpty()) {
            // Create new item with next ID
            val newId = (activeItems + completedItems).maxOfOrNull { it.id }?.plus(1) ?: 1
            val newItem = ToDoItem(newId, text)
            activeItems.add(newItem)  // Add to active list
            inputText = TextFieldValue("")  // Clear input
            showError = false  // Hide error
        } else {
            showError = true  // Show error
            Toast.makeText(context, "Please enter a task", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to toggle task between active and completed
    fun toggleItem(item: ToDoItem) {
        if (item.isCompleted) {
            // Move back to active
            completedItems.remove(item)
            activeItems.add(item.copy(isCompleted = false))
        } else {
            // Move to completed
            activeItems.remove(item)
            completedItems.add(item.copy(isCompleted = true))
        }
    }

    // Function to delete a task
    fun deleteItem(item: ToDoItem, isCompleted: Boolean) {
        if (isCompleted) {
            completedItems.remove(item)
        } else {
            activeItems.remove(item)
        }
    }

    // Main UI layout
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Row for input and add button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    showError = false  // Hide error when typing
                },
                label = { Text("Add a task") },
                modifier = Modifier.weight(1f),  // Take most space
                isError = showError
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { addItem() }) {
                Text("Add")
            }
        }

        // Show error message if needed
        if (showError) {
            Text(
                text = "Task cannot be empty",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active tasks section
        if (activeItems.isNotEmpty()) {
            Text(
                text = "Items",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn {
                items(activeItems) { item ->
                    TodoItemRow(
                        item = item,
                        onToggle = { toggleItem(item) },
                        onDelete = { deleteItem(item, false) }
                    )
                }
            }
        } else {
            Text(
                text = "No items yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Completed tasks section
        if (completedItems.isNotEmpty()) {
            Text(
                text = "Completed Items",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn {
                items(completedItems) { item ->
                    TodoItemRow(
                        item = item,
                        onToggle = { toggleItem(item) },
                        onDelete = { deleteItem(item, true) }
                    )
                }
            }
        } else if (activeItems.isNotEmpty()) {
            Text(
                text = "No completed items yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
fun TodoItemRow(
    item: ToDoItem,
    onToggle: () -> Unit,  // Function to call when checkbox changes
    onDelete: () -> Unit   // Function to call when delete button pressed
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = item.isCompleted,
            onCheckedChange = { onToggle() }  // Call toggle function
        )
        Text(
            text = item.text,
            modifier = Modifier.weight(1f),  // Take remaining space
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(onClick = onDelete) {  // Call delete function
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_close_clear_cancel),
                contentDescription = "Delete"
            )
        }
    }
}