package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.domain.Source

interface GetSourceListUseCase {
    fun getSourceList(): List<Source>
}
