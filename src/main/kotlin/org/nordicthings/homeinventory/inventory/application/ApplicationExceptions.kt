package org.nordicthings.homeinventory.inventory.application

class DuplicateNameException(message: String) : RuntimeException(message)

class EntityNotFoundException(message: String) : RuntimeException(message)

class EntityInUseException(message: String) : RuntimeException(message)
