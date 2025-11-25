# Azure Kubernetes Service Module
resource "azurerm_resource_group" "aks" {
  name     = "${var.prefix}-rg-aks"
  location = var.location

  tags = var.tags
}

resource "azurerm_kubernetes_cluster" "main" {
  name                = "${var.prefix}-aks-${var.random_suffix}"
  location            = azurerm_resource_group.aks.location
  resource_group_name = azurerm_resource_group.aks.name
  dns_prefix          = "${var.prefix}-aks-${var.random_suffix}"
  kubernetes_version  = var.kubernetes_version

  identity {
    type = "SystemAssigned"
  }

  default_node_pool {
    name                = "default"
    vm_size             = var.vm_size
    node_count          = var.node_count
    enable_auto_scaling = true
    min_count           = 1
    max_count           = var.max_node_count
    vnet_subnet_id      = var.subnet_id
  }

 network_profile {
  network_plugin    = "azure"
  load_balancer_sku = "standard"
  service_cidr      = "172.16.0.0/16"
  dns_service_ip    = "172.16.0.10"
 }
  tags = var.tags
}

# Role assignment for AKS to pull images from ACR
resource "azurerm_role_assignment" "acr_pull" {
  principal_id                     = azurerm_kubernetes_cluster.main.identity[0].principal_id
  role_definition_name             = "AcrPull"
  scope                            = var.acr_id
  skip_service_principal_aad_check = true
}

