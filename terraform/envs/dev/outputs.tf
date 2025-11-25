# Outputs for dev environment

output "acr_login_server" {
  description = "ACR login server URL"
  value       = module.acr.acr_login_server
}

output "acr_name" {
  description = "ACR name"
  value       = module.acr.acr_name
}

output "aks_get_credentials_command" {
  description = "Command to get AKS credentials"
  value       = module.aks.get_credentials_command
}

output "aks_name" {
  description = "AKS cluster name"
  value       = module.aks.aks_name
}

output "aks_resource_group" {
  description = "AKS resource group name"
  value       = "${var.prefix}-rg-aks"
}

output "sql_server_host" {
  description = "SQL Server hostname (FQDN)"
  value       = module.database.sql_server_host
}

output "sql_server_fully_qualified_domain_name" {
  description = "SQL Server fully qualified domain name"
  value       = module.database.sql_server_fully_qualified_domain_name
}

output "sql_server_name" {
  description = "SQL Server name"
  value       = module.database.sql_server_name
}

output "database_name" {
  description = "Database name"
  value       = module.database.database_name
}

output "vnet_name" {
  description = "Virtual network name"
  value       = module.networking.vnet_name
}

