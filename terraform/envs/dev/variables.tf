variable "prefix" {
  description = "Prefix for resource names"
  type        = string
  default     = "ecommerce"
}

variable "location" {
  description = "Azure region"
  type        = string
  default     = "East US"
}

variable "random_suffix" {
  description = "Random suffix for unique resource names (to avoid conflicts)"
  type        = string
  default     = "dev001"
}

variable "sql_admin_password" {
  description = "Password for SQL Server administrator"
  type        = string
  sensitive   = true
}

variable "tags" {
  description = "Tags to apply to all resources"
  type        = map(string)
  default = {
    Environment = "dev"
    Project     = "ecommerce-microservices"
    ManagedBy   = "terraform"
  }
}

