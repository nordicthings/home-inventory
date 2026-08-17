package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ItemJpaEntityRepository : JpaRepository<ItemJpaEntity, UUID> {
    fun findByNormalizedName(normalizedName: String): ItemJpaEntity?

    fun existsByCategoryId(categoryId: UUID): Boolean

    @Query(
        """
        select distinct item
        from ItemJpaEntity item
        where (:normalizedNameContains is null or item.normalizedName like concat('%', :normalizedNameContains, '%'))
          and (:categoryId is null or item.categoryId = :categoryId)
          and (
              :locationId is null
              or exists (
                  select locationQuantity
                  from ItemLocationQuantityJpaEntity locationQuantity
                  where locationQuantity.id.itemId = item.id
                    and locationQuantity.id.locationId = :locationId
                    and locationQuantity.quantity > 0
              )
          )
          and (
              :sourceId is null
              or exists (
                  select itemSource
                  from ItemSourceJpaEntity itemSource
                  where itemSource.itemId = item.id
                    and itemSource.sourceId = :sourceId
              )
          )
        order by item.normalizedName asc
        """,
    )
    fun search(
        @Param("normalizedNameContains") normalizedNameContains: String?,
        @Param("categoryId") categoryId: UUID?,
        @Param("locationId") locationId: UUID?,
        @Param("sourceId") sourceId: UUID?,
    ): List<ItemJpaEntity>
}
