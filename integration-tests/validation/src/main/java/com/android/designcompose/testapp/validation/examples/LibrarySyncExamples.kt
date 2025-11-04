/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.designcompose.testapp.validation.examples

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.android.designcompose.ComponentReplacementContext
import com.android.designcompose.getComponentId
import com.android.designcompose.getComponentName
import com.android.designcompose.getComponentSetName
import com.android.designcompose.getOverridesMap

/**
 * Example demonstrating how to access component override information for library synchronization.
 * 
 * This example shows:
 * 1. How to access componentInfo from ComponentReplacementContext
 * 2. How to inspect calculated overrides
 * 3. How to use override information for library sync workflows
 */

@Composable
fun ComponentWithOverrideInfo(
    context: ComponentReplacementContext,
    label: String = "Component"
) {
    // Access component metadata
    val componentId = context.getComponentId()
    val componentName = context.getComponentName()
    val componentSetName = context.getComponentSetName()
    val overrides = context.getOverridesMap()
    
    // Log override information for debugging
    Log.d("LibrarySync", "=== Component Replacement: $label ===")
    Log.d("LibrarySync", "Component ID: $componentId")
    Log.d("LibrarySync", "Component Name: $componentName")
    Log.d("LibrarySync", "Component Set: $componentSetName")
    Log.d("LibrarySync", "Overrides count: ${overrides.size}")
    
    overrides.forEach { (key, override) ->
        Log.d("LibrarySync", "Override key: $key")
        if (override.hasStyle()) {
            Log.d("LibrarySync", "  - Has style overrides")
        }
        if (override.hasViewData()) {
            Log.d("LibrarySync", "  - Has view data overrides")
        }
    }
    
    // Render a debug view showing the override information
    Column(
        modifier = context.layoutModifier
            .background(Color(0xFFE3F2FD))
            .padding(8.dp)
    ) {
        Text("Component: ${componentName ?: "Unknown"}")
        Text("ID: ${componentId ?: "N/A"}", modifier = Modifier.padding(top = 4.dp))
        
        if (componentSetName != null) {
            Text("Set: $componentSetName", modifier = Modifier.padding(top = 4.dp))
        }
        
        if (overrides.isNotEmpty()) {
            Text(
                "Overrides: ${overrides.size}",
                modifier = Modifier.padding(top = 4.dp),
                color = Color(0xFF1976D2)
            )
            overrides.entries.take(3).forEach { (key, override) ->
                val types = buildList {
                    if (override.hasStyle()) add("style")
                    if (override.hasViewData()) add("data")
                }.joinToString(", ")
                Text(
                    "  • $key: $types",
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
            if (overrides.size > 3) {
                Text(
                    "  ... and ${overrides.size - 3} more",
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        } else {
            Text(
                "No overrides detected",
                modifier = Modifier.padding(top = 4.dp),
                color = Color.Gray
            )
        }
    }
}

/**
 * Example of using componentInfo for library synchronization.
 * 
 * In a real application, you would:
 * 1. Check if componentId is present
 * 2. Fetch the main component from the library using the componentId
 * 3. Apply the overrides from context.getOverridesMap() to the library component
 * 4. Render the library component with overrides applied
 */
@Composable
fun LibrarySyncExample(context: ComponentReplacementContext) {
    val componentId = context.getComponentId()
    
    if (componentId != null) {
        // TODO: Implement library component fetching
        // val libraryComponent = fetchLibraryComponent(componentId)
        // val overrides = context.getOverridesMap()
        // renderWithOverrides(libraryComponent, overrides)
        
        // For now, show what we have
        ComponentWithOverrideInfo(context, "Library Component")
    } else {
        // Not a component instance, render normally
        Box(modifier = context.layoutModifier) {
            Text("Not a component instance")
        }
    }
}

/**
 * Example showing how to conditionally use library sync based on override presence.
 */
@Composable
fun ConditionalLibrarySyncExample(context: ComponentReplacementContext) {
    val overrides = context.getOverridesMap()
    val componentId = context.getComponentId()
    
    when {
        // If no overrides and we have a component ID, use the library version directly
        overrides.isEmpty() && componentId != null -> {
            Log.d("LibrarySync", "Using library component directly (no overrides)")
            // Fetch and render library component
            ComponentWithOverrideInfo(context, "Pure Library Component")
        }
        
        // If we have overrides, fetch library component and apply them
        overrides.isNotEmpty() && componentId != null -> {
            Log.d("LibrarySync", "Using library component with ${overrides.size} override(s)")
            // Fetch library component and apply overrides
            ComponentWithOverrideInfo(context, "Library Component + Overrides")
        }
        
        // Otherwise, use the original component
        else -> {
            Log.d("LibrarySync", "Using original component")
            ComponentWithOverrideInfo(context, "Original Component")
        }
    }
}

/**
 * Demonstrates accessing specific override types.
 */
@Composable
fun OverrideInspectionExample(context: ComponentReplacementContext) {
    val overrides = context.getOverridesMap()
    val componentSetName = context.getComponentSetName()
    
    // Check for root component overrides
    val rootOverride = componentSetName?.let { overrides[it] }
    
    Column(modifier = context.layoutModifier.padding(8.dp)) {
        Text("Override Inspection", color = Color(0xFF1976D2))
        
        if (rootOverride != null) {
            Text("Root Override Found:", modifier = Modifier.padding(top = 8.dp))
            
            if (rootOverride.hasStyle()) {
                val style = rootOverride.style
                Text("  Style overrides present", modifier = Modifier.padding(start = 8.dp))
                // Access specific style properties
                // style.background, style.border, style.layout, etc.
            }
            
            if (rootOverride.hasViewData()) {
                val viewData = rootOverride.viewData
                Text("  ViewData overrides present", modifier = Modifier.padding(start = 8.dp))
                // Access view data based on type
                // Container, Text, StyledTextRuns, etc.
            }
        } else {
            Text("No root override", modifier = Modifier.padding(top = 8.dp), color = Color.Gray)
        }
        
        // Check for descendant overrides
        val descendantOverrides = overrides.filter { it.key != componentSetName }
        if (descendantOverrides.isNotEmpty()) {
            Text(
                "Descendant Overrides: ${descendantOverrides.size}",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
