# Library Synchronization Implementation Notes

## Overview
This implementation adds support for exposing component override information to enable library synchronization workflows in DesignCompose.

## Changes Made

### 1. Enhanced ComponentReplacementContext Interface
**File:** `designcompose/src/main/java/com/android/designcompose/CustomizationContext.kt`

Added `componentInfo` property to the `ComponentReplacementContext` interface:
```kotlin
interface ComponentReplacementContext {
    val layoutModifier: Modifier
    val textStyle: TextStyle?
    val componentInfo: ComponentInfo?  // NEW - exposes component metadata and overrides
}
```

This allows component replacement functions to access:
- Component ID (for fetching from library)
- Component name and component set name
- **Calculated overrides** via `componentInfo.overridesTableMap`

### 2. Added Helper Extension Functions
Added convenience functions to easily access override information:
- `getOverridesMap()` - Returns the full overrides table
- `getComponentId()` - Returns the component ID
- `getComponentName()` - Returns the component name
- `getComponentSetName()` - Returns the component set name

### 3. Updated Internal Implementation
**Files:** 
- `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshTreeBuilder.kt`
- `designcompose/src/main/java/com/android/designcompose/squoosh/SquooshRoot.kt`

- Added `componentInfo` field to `SquooshChildComposable` class
- Updated component replacement instantiation to pass componentInfo
- Updated `ComponentReplacementContext` implementation to expose componentInfo

### 4. Added Diagnostic Logging
**File:** `crates/figma_import/src/document.rs`

Added debug logging in the override calculation logic to help diagnose issues:
- Logs when reference component is not found for an instance
- Logs when overrides are recorded (with type info)
- Logs when no overrides are found (style/data matches)

## Usage Example

### Accessing Override Information
```kotlin
CustomizationContext().apply {
    setComponent("MyButton") { context ->
        // Access component metadata
        val componentId = context.componentInfo?.id
        val componentName = context.componentInfo?.name
        
        // Access calculated overrides
        val overrides = context.getOverridesMap()
        
        // Check if there are any overrides
        if (overrides.isNotEmpty()) {
            Log.d("LibrarySync", "Found ${overrides.size} override(s)")
            overrides.forEach { (key, override) ->
                Log.d("LibrarySync", "Override key: $key")
                if (override.hasStyle()) {
                    Log.d("LibrarySync", "  - Has style overrides")
                }
                if (override.hasViewData()) {
                    Log.d("LibrarySync", "  - Has view data overrides")
                }
            }
        }
        
        // Use the layoutModifier to maintain layout
        Box(modifier = context.layoutModifier) {
            // Your replacement content
        }
    }
}
```

### Debugging Override Calculation
To enable diagnostic logging, set the Rust log level to debug. The logs will show:
- Which component instances don't have matching reference components
- Which overrides are being recorded
- Which instances have no overrides (matching style/data)

## Known Issues and Next Steps

### Issue: Empty or Incorrect Overrides
Based on user feedback, there are cases where:
1. **Empty overrides**: Instance has visible changes (e.g., red button) but `overridesTableMap` is empty
2. **Extraneous data**: Nested replacements show large objects with unexpected data

### Root Cause Analysis
The override calculation in `document.rs` (lines 554-696) works by:
1. Finding the reference component using NodeQuery (by ID, name, or variant)
2. Comparing instance ViewStyle and ViewData with reference component
3. Recording differences in the overrides table

Potential issues:
- **Missing reference component**: If the library component isn't in `reference_components` HashMap, no overrides are calculated
- **Query mismatch**: The NodeQuery lookup might not find the correct reference component
- **Comparison logic**: The ViewStyle/ViewData `difference()` method might not detect all overrides

### Diagnostic Steps
1. Enable debug logging and check for "no reference component found" messages
2. Verify that library components are being fetched and indexed
3. Check if the correct reference component is being used for comparison
4. Examine the ViewStyle and ViewData difference calculations

### Future Enhancements
1. **Automatic library fetching**: Enhance the system to automatically fetch main components from libraries
2. **Override application utilities**: Create helper functions to apply overrides to views
3. **Library sync mode**: Add a flag to automatically use library components instead of instances
4. **Better error reporting**: Provide clear warnings when overrides can't be calculated

## Testing Recommendations

To test the changes:
1. Create a Figma library with a component (e.g., a Button)
2. Create a spec file that uses instances of that component
3. Apply overrides to the instance (e.g., change color, text, size)
4. Use Component Replacement with the new componentInfo
5. Check the logs and overridesTableMap to verify override detection

## Technical Details

### Override Calculation Flow
1. Document fetching (`fetch_component_variants()`) retrieves external library components
2. Components are indexed in various HashMaps (by ID, name, variant)
3. Views are created from Figma nodes (`create_component_flexbox()`)
4. `compute_component_overrides()` compares instances with their reference components
5. Overrides are stored in `ComponentInfo.overrides_table` (a proto map)
6. This flows to Kotlin as `componentInfo.overridesTableMap`

### Key Data Structures
- `ComponentInfo` (proto): Contains id, name, component_set_name, overrides_table
- `ComponentOverrides` (proto): Contains style and view_data overrides
- `ViewStyle`: Layout, positioning, visual styling
- `ViewData`: Type-specific data (Container, Text, etc.)

## Related Files
- Proto definitions: `crates/dc_bundle/src/proto/definition/view/view.proto`
- View conversion: `crates/figma_import/src/transform_flexbox.rs`
- Style differences: `crates/dc_bundle/src/definition/view.rs`
