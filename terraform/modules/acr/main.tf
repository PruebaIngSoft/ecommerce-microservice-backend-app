# Azure Container Registry Module
resource "azurerm_resource_group" "acr" {
  name     = "${var.prefix}-rg-acr"
  location = var.location

  tags = var.tags
}

resource "azurerm_container_registry" "main" {
  name                = "${var.prefix}acr${var.random_suffix}"
  resource_group_name = azurerm_resource_group.acr.name
  location            = azurerm_resource_group.acr.location
  sku                 = "Basic"
  admin_enabled       = true

  tags = var.tags
}

