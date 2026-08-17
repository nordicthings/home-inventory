package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.domain.Category

interface GetCategoryListUseCase {
    fun getCategoryList(): List<Category>
}
