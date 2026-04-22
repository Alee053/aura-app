# KMP Strings Extraction Design

## Goal
Extract hardcoded strings from TodoScreen and TodoItem composables into `strings.xml` following KMP pattern.

## File Structure

**New file:**
- `composeApp/src/commonMain/composeResources/values/strings.xml`

**Updated files:**
- `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/screen/TodoScreen.kt`
- `composeApp/src/commonMain/kotlin/com/programovil/aura/todo/presentation/composable/TodoItem.kt`

## Strings to Extract

| ID | Value |
|---|---|
| `todos_title` | My Todos |
| `add_todo` | + |
| `new_todo_hint` | New todo |
| `empty_todos` | No todos yet. Add your first one! |
| `delete` | X |

## Implementation

1. Create `strings.xml` with all string resources
2. Update `TodoScreen.kt`:
   - Replace `"My Todos"` → `stringResource(Res.string.todos_title)`
   - Replace `"+"` → `stringResource(Res.string.add_todo)`
   - Replace `"New todo"` → `stringResource(Res.string.new_todo_hint)`
   - Replace `"No todos yet. Add your first one!"` → `stringResource(Res.string.empty_todos)`
3. Update `TodoItem.kt`:
   - Replace `"X"` → `stringResource(Res.string.delete)`
4. Add imports to both files:
   ```kotlin
   import aura_app.composeapp.generated.resources.Res
   import aura_app.composeapp.generated.resources.<string_id>
   import org.jetbrains.compose.resources.stringResource
   ```

## Scope
- UI/composable strings only (not ViewModel error messages)
