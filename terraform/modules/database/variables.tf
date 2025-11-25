variable "prefix" {
  description = "Prefix for resource names"
  type        = string
}

variable "location" {
  description = "Azure region"
  type        = string
}

variable "random_suffix" {
  description = "Random suffix for unique resource names"
  type        = string
  default     = "001"
}

variable "administrator_login" {
  description = "Administrator login for SQL Server"
  type        = string
  default     = "sqladmin"
}

variable "administrator_password" {
  description = "Administrator password for SQL Server"
  type        = string
  sensitive   = true
}

variable "sku_name" {
  description = "SKU name for SQL Database (Basic or S0 for low cost)"
  type        = string
  default     = "Basic"
}

variable "database_name" {
  description = "Name of the database to create"
  type        = string
  default     = "ecommerce"
}

variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}

