package org.nordicthings.homeinventory.inventory.application

class DuplicateNameException(message: String) : RuntimeException(message)

class EntityNotFoundException(message: String) : RuntimeException(message)

class EntityInUseException(message: String) : RuntimeException(message)

class AcquisitionInvoiceAlreadyExistsException(
    val existingFilename: String,
) : RuntimeException("Acquisition invoice already exists: $existingFilename")

class InvalidAcquisitionInvoiceException(message: String) : RuntimeException(message)
