# Todo Creation/Editing Dialog Design Spec

**Date:** 2025-05-21  
**Scope:** Todos feature UX improvement — replace inline creation with a reusable dialog that supports editing, descriptions, and due-date management.  
**Approach:** A (Standard Dialog with nested DatePickerDialog)

---

## 1. Goal

Improve the Todos screen UX by:
- Removing the inline `OutlinedTextField` creation input at the top.
- Introducing a reusable `TodoDialog` for both **creating** and **editing** todos.
- Adding an optional `description` field to todos.
- Moving delete action into the edit dialog to reduce accidental deletions.
- Adding an "Add first todo" button in the empty state.

## 2. Model Changes

### `Todo` domain model
```kotlin
data class Todo(
    val id: String,
    val title: String,
    val description: String? = null,   // NEW
    val isCompleted: Boolean = false,
    val dueDate: Long? = null
)
```

### `TodoData` mapper
Add `description: String?` field. Firestore read/write must map it. Existing documents without the field default to `null` (backward-compatible).

## 3. UI Architecture

### 3.1 `TodoDialog` (new composable)

**Location:** `todo/presentation/composable/TodoDialog.kt`

**Signature:**
```kotlin
@Composable
fun TodoDialog(
    todo: Todo?,                       // null = create mode; non-null = edit mode
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?, dueDate: Long?) -> Unit,
    onDelete: (() -> Unit)? = null   // shown only in edit mode
)
```

**Layout (top to bottom):**
1. **Dialog title** — `new_todo` string when creating, `edit_todo` when editing.
2. **Name field** (`BasicInput`) — required, single line, label `todo_name_hint`.
3. **Description field** (`BasicInput`) — optional, multi-line (max 3 lines), label `todo_description_hint`.
4. **Due date row** — clickable row showing:
   - Current due date formatted (if set)
   - `add_due_date` placeholder (if not set)
   - Trailing "x" icon to clear the date (if set)
   Tapping the row opens a nested `DatePickerDialog`.
5. **Actions** — horizontally arranged:
   - Left side (edit mode only): TextButton "Delete" → invokes `onDelete`.
   - Right side: "Cancel" (PrimaryButton) + "Save" (PrimaryButton, disabled while title is blank).

**Design-system compliance:**
- All colors via `AppTheme.colors.*`.
- All typography via `AppTheme.typography.*`.
- All strings externalized in `strings.xml`.

### 3.2 `TodoItem` changes

**Location:** `todo/presentation/composable/TodoItem.kt`

- Add `onClick: () -> Unit` parameter.
- Make the `Card` clickable (`Modifier.clickable { onClick() }`).
- **Remove** the delete `IconButton` from the item (moved into edit dialog).
- Display `description` (if present) below the title in `textSecondary` / `labelLarge` style, max 2 lines, ellipsized.
- Keep checkbox and due-date label.

### 3.3 `TodoScreen` changes

**Location:** `todo/presentation/screen/TodoScreen.kt`

- **Remove:**
  - Inline `OutlinedTextField` and its state (`newTodoTitle`).
  - Date picker inline trigger and `selectedDueDate` label.
  - FAB logic that directly adds a todo.
- **FAB** `onClick` → opens `TodoDialog` in create mode.
- **Empty state** (`todos.isEmpty()`) → shows centered text + a `PrimaryButton` labeled `add_first_todo` that opens the create dialog.
- **List items** pass `onClick = { open dialog in edit mode with this todo }`.
- Dialog state managed with `var showDialog by remember { mutableStateOf(false) }` and `var editingTodo by remember { mutableStateOf<Todo?>(null) }`.

## 4. Data Layer Changes

### `TodoRepository` interface
```kotlin
@Mockable
interface TodoRepository {
    fun getTodos(): Flow<Result<List<Todo>>>
    suspend fun addTodo(title: String, description: String?, dueDate: Long? = null): Result<Unit>
    suspend fun updateTodo(todo: Todo): Result<Unit>            // NEW
    suspend fun toggleTodo(todoId: String, isCompleted: Boolean): Result<Unit>
    suspend fun deleteTodo(todoId: String): Result<Unit>
}
```

### Android implementation (`TodoRepositoryImpl`)
- `addTodo`: include `"description"` in the Firestore map (only if non-null).
- `updateTodo`: Firestore `document(todoId).update(...)` with `title`, `description`, `dueDate` fields.
- `getTodos`: read `"description"` from snapshot (nullable).

### iOS stub (`IosTodoRepositoryImpl`)
- Stub `updateTodo` returning `Result.success(Unit)`.
- Update `addTodo` signature to accept `description`.

### Use cases
- `AddTodoUseCase`: signature updated to `invoke(title, description, dueDate)`.
- **New:** `UpdateTodoUseCase` (`todo/presentation/domain/usecase/UpdateTodoUseCase.kt`) — delegates to `repository.updateTodo(todo)`.

### DI (`TodoModule`)
- Register `UpdateTodoUseCase`.
- Inject into `TodoViewModel`.

## 5. ViewModel Changes

### `TodoViewModel`
- New dependency: `updateTodoUseCase: UpdateTodoUseCase`.
- `addTodo(title, description, dueDate)` — passes `description`.
- `updateTodo(todo: Todo)` — calls `updateTodoUseCase`, sets error on failure.

## 6. String Resources

New entries in `composeApp/src/commonMain/composeResources/values/strings.xml`:

| Key | Value |
|-----|-------|
| `new_todo` | New Todo |
| `edit_todo` | Edit Todo |
| `todo_name_hint` | Todo name |
| `todo_description_hint` | Description (optional) |
| `add_due_date` | Add due date |
| `clear_due_date` | Clear |
| `add_first_todo` | Add first todo |

## 7. Testing Impact

- `TodoViewModel` tests must be updated to pass `description` to `addTodo` and cover `updateTodo`.
- `AddTodoUseCase` tests updated for new signature.
- New `UpdateTodoUseCaseTest`.
- Mockative mocks for `TodoRepository` regenerated (interface changed).

## 8. Out of Scope

- Habit screen changes (covered in future work).
- Local persistence migration (Firestore remains source of truth).
- iOS repository real implementation.

## 9. Design System Checklist

- [ ] Backgrounds: `TodoDialog` `Surface` uses `AppTheme.colors.surface`; `TodoScreen` root uses `AppTheme.colors.background`.
- [ ] Loading states: `CircularProgressIndicator` sits on `AppTheme.colors.background`.
- [ ] Icons: All `Icon` composables use explicit `tint` tokens.
- [ ] Buttons: Destructive "Delete" uses `AppTheme.colors.error`; primary actions use `AppTheme.colors.primary`.
- [ ] Text strings as icons: No character-based icons used.
