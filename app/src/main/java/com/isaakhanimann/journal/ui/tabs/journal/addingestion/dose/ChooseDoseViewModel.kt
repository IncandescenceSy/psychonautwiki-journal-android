/*
 * Copyright (c) 2022-2023. Isaak Hanimann.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * PsychonautWiki Journal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PsychonautWiki Journal.  If not, see https://www.gnu.org/licenses/gpl-3.0.en.html.
 */

package com.isaakhanimann.journal.ui.tabs.journal.addingestion.dose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.Substance
import com.isaakhanimann.journal.data.substances.classes.roa.DoseClass
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDose
import com.isaakhanimann.journal.data.substances.repositories.SubstanceRepository
import com.isaakhanimann.journal.ui.main.navigation.routers.ADMINISTRATION_ROUTE_KEY
import com.isaakhanimann.journal.ui.main.navigation.routers.SUBSTANCE_NAME_KEY
import com.isaakhanimann.journal.ui.tabs.search.substance.roa.toReadableString
import com.isaakhanimann.journal.ui.tabs.settings.combinations.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChooseDoseViewModel @Inject constructor(
    repository: SubstanceRepository,
    state: SavedStateHandle,
    private val userPreferences: UserPreferences
) : ViewModel() {
    val substance: Substance = repository.getSubstance(state.get<String>(SUBSTANCE_NAME_KEY)!!)!!
    val administrationRoute: AdministrationRoute
    val roaDose: RoaDose?
    var isEstimate by mutableStateOf(false)
    var doseText by mutableStateOf("")
    var estimatedDoseStandardDeviationText by mutableStateOf("")
    var purityText by mutableStateOf("100")
    var units by mutableStateOf("")
    private val purity: Double?
        get() {
            val p = purityText.toDoubleOrNull()
            return if (p != null && p > 0 && p <= 100) {
                p
            } else {
                null
            }
        }
    val isPurityValid: Boolean get() = purity != null
    val impureDoseWithUnit: String?
        get() {
            dose.let {
                if (it == null) return null
                purity.let { safePurity ->
                    if (safePurity == null) return null
                    val result = it.div(safePurity).times(100)
                    return result.toReadableString() + " impure ${roaDose?.units ?: ""}"
                }
            }
        }
    val dose: Double? get() = doseText.toDoubleOrNull()
    val estimatedDoseStandardDeviation: Double? get() = estimatedDoseStandardDeviationText.toDoubleOrNull()
    val isValidDose: Boolean get() = dose != null
    val currentDoseClass: DoseClass? get() = roaDose?.getDoseClass(ingestionDose = dose)

    val isCustomUnitHintShown = userPreferences.isCustomUnitHintShownFlow.stateIn(
        initialValue = false,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun hideCustomUnitsHint() = viewModelScope.launch {
        userPreferences.hideCustomUnitsHint()
    }

    fun onDoseTextChange(newDoseText: String) {
        doseText = newDoseText.replace(oldChar = ',', newChar = '.')
    }

    fun onEstimatedDoseStandardDeviationChange(newEstimatedStandardDeviationText: String) {
        estimatedDoseStandardDeviationText = newEstimatedStandardDeviationText.replace(oldChar = ',', newChar = '.')
    }

    init {
        val routeString = state.get<String>(ADMINISTRATION_ROUTE_KEY)!!
        administrationRoute = AdministrationRoute.valueOf(routeString)
        roaDose = substance.getRoa(administrationRoute)?.roaDose
        units = roaDose?.units ?: ""
    }

}
