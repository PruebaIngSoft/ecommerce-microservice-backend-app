# Networking Module - VNet and Subnets
resource "azurerm_resource_group" "networking" {
  name     = "${var.prefix}-rg-networking"
  location = var.location

  tags = var.tags
}

resource "azurerm_virtual_network" "main" {
  name                = "${var.prefix}-vnet"
  address_space       = var.address_space
  location            = azurerm_resource_group.networking.location
  resource_group_name = azurerm_resource_group.networking.name

  tags = var.tags
}

resource "azurerm_subnet" "aks" {
  name                 = "${var.prefix}-subnet-aks"
  resource_group_name  = azurerm_resource_group.networking.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = var.aks_subnet_address_prefixes
}

resource "azurerm_subnet" "database" {
  name                 = "${var.prefix}-subnet-database"
  resource_group_name  = azurerm_resource_group.networking.name
  virtual_network_name = azurerm_virtual_network.main.name
  address_prefixes     = var.database_subnet_address_prefixes
}

