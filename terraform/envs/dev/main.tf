# Main configuration for dev environment
# This file orchestrates all modules

# Networking Module
module "networking" {
  source = "../../modules/networking"

  prefix  = var.prefix
  location = var.location
  tags     = var.tags
}

# Azure Container Registry Module
module "acr" {
  source = "../../modules/acr"

  prefix        = var.prefix
  location      = var.location
  random_suffix = var.random_suffix
  tags          = var.tags
}

# Azure Kubernetes Service Module
module "aks" {
  source = "../../modules/aks"

  prefix        = var.prefix
  location      = var.location
  random_suffix = var.random_suffix
  subnet_id     = module.networking.aks_subnet_id
  acr_id        = module.acr.acr_id
  tags          = var.tags

  depends_on = [
    module.networking,
    module.acr
  ]
}

# Database Module
module "database" {
  source = "../../modules/database"

  prefix                = var.prefix
  location              = var.location
  random_suffix         = var.random_suffix
  administrator_password = var.sql_admin_password
  tags                  = var.tags
}

