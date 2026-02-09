# =============================================================================
# Dev Environment Outputs
# =============================================================================

# -----------------------------------------------------------------------------
# VPC Outputs
# -----------------------------------------------------------------------------
output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "vpc_cidr_block" {
  description = "VPC CIDR 블록"
  value       = module.vpc.vpc_cidr_block
}

output "public_subnet_ids" {
  description = "Public Subnet ID 목록 (ALB용)"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private Subnet ID 목록 (ECS, RDS용)"
  value       = module.vpc.private_subnet_ids
}

output "availability_zones" {
  description = "사용 중인 가용 영역"
  value       = module.vpc.availability_zones
}
