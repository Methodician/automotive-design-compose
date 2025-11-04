# Troubleshooting Library Synchronization Override Issues

## Problem: Empty overridesTableMap

If you're seeing an empty `overridesTableMap` when you know overrides were applied in Figma:

### Possible Causes

1. **Reference Component Not Found**
   - The main component from the library isn't in the `reference_components` HashMap
   - This happens if the component couldn't be fetched or wasn't indexed properly

2. **Query Mismatch**
   - The NodeQuery lookup (by ID, name, or variant) doesn't match the reference component
   - Component IDs or names changed between spec and library

3. **Override Detection Logic**
   - The ViewStyle/ViewData comparison might not detect certain types of overrides
   - Some Figma properties might not map to ViewStyle/ViewData fields

### Diagnostic Steps

1. **Enable Debug Logging**
   ```bash
   # Set Rust log level to debug
   adb shell setprop log.tag.RustStdoutStderr DEBUG
   
   # Or set RUST_LOG environment variable before running
   export RUST_LOG=debug
   ```

2. **Check for "No Reference Component Found" Messages**
   Look for log lines like:
   ```
   Component instance 'MyButton' (id: xxx, component_set: yyy) - no reference component found
   ```
   
   This indicates the main component isn't available for comparison.

3. **Verify Component Fetching**
   - Check if the library component file is being fetched
   - Look for HTTP requests to Figma API for component data
   - Verify network connectivity and API token validity

4. **Inspect Component IDs**
   ```kotlin
   setComponent("MyButton") { context ->
       val componentId = context.getComponentId()
       val componentName = context.getComponentName()
       Log.d("Debug", "Component ID: $componentId, Name: $componentName")
       
       // Check if these match what you expect from Figma
   }
   ```

5. **Check Reference Components HashMap**
   - Verify that library components are in the nodes HashMap
   - Check that variant_nodes are being indexed properly

## Problem: Extraneous Data in overridesTableMap

If you're seeing a large overridesTableMap with unexpected data:

### Possible Causes

1. **Wrong Reference Component**
   - The comparison is happening against the wrong reference component
   - For nested components, the lookup might find a parent instead of the actual component

2. **Spurious Differences**
   - ViewStyle/ViewData difference calculation is finding differences that aren't real overrides
   - Float precision issues in layout calculations
   - Default values being treated as overrides

3. **Nested Component Instance Issues**
   - When replacing a component that contains other component instances
   - The child instances might have their overrides calculated against the wrong reference

### Diagnostic Steps

1. **Check Reference Component Matching**
   ```rust
   // In document.rs, the queries are:
   NodeQuery::NodeId(info.id.clone())
   NodeQuery::NodeName(info.name.clone())
   NodeQuery::NodeVariant(info.name.clone(), info.component_set_name.clone())
   ```
   
   Verify which query is matching and if it's finding the correct component.

2. **Examine Override Keys**
   ```kotlin
   setComponent("MyCard") { context ->
       val overrides = context.getOverridesMap()
       overrides.forEach { (key, override) ->
           Log.d("Debug", "Override key: $key")
           // Keys should be either component_set_name (root) or view_name (descendants)
           // If you see unexpected keys, the comparison is wrong
       }
   }
   ```

3. **Compare Style/Data Directly**
   - Log the actual ViewStyle and ViewData being compared
   - Check if the difference calculation is finding real differences

4. **Check for Float Precision Issues**
   - Layout values (x, y, width, height) might have tiny floating-point differences
   - These shouldn't be treated as overrides if they're within epsilon

## Problem: Nested Component Overrides

When replacing a component that contains sub-components that are also being replaced:

### Understanding the Issue

1. **Instance Hierarchy**
   ```
   Spec File:
     CardInstance (from Library)
       ButtonInstance (from Library) - RED override
       ButtonInstance (from Library) - BLUE override
   
   Library File:
     Card Component
       Button Component
       Button Component
   ```

2. **Override Calculation**
   - The Card instance gets compared to Card component
   - Each Button instance inside gets compared to Button component
   - BUT: If you replace Card with library version, the Buttons inside might get wrong overrides

### Solution Approach

For nested replacements:
1. Calculate overrides for the root component (Card)
2. Calculate overrides for each child component instance (Buttons)
3. When replacing Card with library version, need to also replace Buttons and apply their overrides

### Code Example

```kotlin
// Custom function to handle nested library sync
@Composable
fun NestedLibrarySync(context: ComponentReplacementContext) {
    val rootOverrides = context.getOverridesMap()
    val componentId = context.getComponentId()
    
    if (componentId != null) {
        // 1. Fetch library component
        // val libraryComponent = fetchComponent(componentId)
        
        // 2. Apply root overrides
        // applyOverrides(libraryComponent, rootOverrides)
        
        // 3. For each child component instance in libraryComponent:
        //    - Find corresponding override in rootOverrides
        //    - Apply that override to the child
        
        // 4. Render the modified library component
    }
}
```

## Common Scenarios

### Scenario 1: Simple Style Override (e.g., Red Button)

**Expected**: Override contains style with color change
**If Empty**: Reference component not found or style comparison failed
**Fix**: Verify component fetching and style comparison logic

### Scenario 2: Text Override

**Expected**: Override contains view_data with text content
**If Empty**: ViewData comparison might not detect text changes
**Fix**: Check ViewData difference calculation for Text type

### Scenario 3: Layout Override (Size/Position)

**Expected**: Override contains style with layout changes
**If Empty**: Layout values might match exactly (no override)
**Note**: Only absolute positioning changes create overrides, not auto-layout changes

### Scenario 4: Variant Selection Override

**Expected**: The variant selection itself isn't an override
**Behavior**: Different variant = different reference component
**Note**: Overrides are calculated against the selected variant

## Debugging Workflow

1. **Start Simple**
   - Test with a single component instance
   - Apply one obvious override (e.g., change fill color)
   - Check if override is detected

2. **Add Complexity**
   - Test with nested components
   - Test with multiple overrides
   - Test with different override types (style, text, layout)

3. **Compare Expected vs Actual**
   - Document what you override in Figma
   - Log what appears in overridesTableMap
   - Identify which overrides are missing or wrong

4. **Isolate the Issue**
   - If simple case works but complex doesn't → nested component issue
   - If no overrides detected at all → reference component issue
   - If wrong overrides detected → comparison or query issue

## Next Steps

If issues persist after trying these troubleshooting steps:

1. **Collect Diagnostic Information**
   - Component IDs and names from Figma
   - Structure of your library and spec files
   - Which overrides you applied
   - What overridesTableMap contains
   - Debug logs from document.rs

2. **Create Minimal Reproduction**
   - Simple Figma file with one library component
   - One instance with one override
   - Test if override is detected

3. **Report Issue**
   - Include all diagnostic information
   - Include minimal reproduction case
   - Include debug logs

## Workarounds

While investigating root cause:

1. **Manual Override Application**
   ```kotlin
   setComponent("MyButton") { context ->
       // Manually specify the overrides you know exist
       val backgroundColor = Color.Red  // You know this from Figma
       Box(modifier = context.layoutModifier.background(backgroundColor)) {
           // Content
       }
   }
   ```

2. **Hybrid Approach**
   - Use library component for structure
   - Use component replacement for customization
   - Don't rely on automatic override detection

3. **Separate Customization**
   - Use DesignCompose customization APIs
   - Apply overrides through Kotlin code
   - Don't use component replacement for library sync
