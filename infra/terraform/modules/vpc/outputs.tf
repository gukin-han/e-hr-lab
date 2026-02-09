# =============================================================================
# VPC Module - Outputs
# =============================================================================
# 다른 모듈에서 VPC 정보를 참조할 때 사용합니다.
# 예: Security Groups, ECS, RDS, ALB 모듈 등
# =============================================================================

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "vpc_cidr_block" {
  description = "VPC CIDR 블록"
  value       = aws_vpc.main.cidr_block
}

output "public_subnet_ids" {
  description = "Public Subnet ID 목록 (ALB용)"
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private Subnet ID 목록 (ECS, RDS용)"
  value       = aws_subnet.private[*].id
}

output "public_subnet_cidrs" {
  description = "Public Subnet CIDR 목록"
  value       = aws_subnet.public[*].cidr_block
}

output "private_subnet_cidrs" {
  description = "Private Subnet CIDR 목록"
  value       = aws_subnet.private[*].cidr_block
}

output "internet_gateway_id" {
  description = "Internet Gateway ID"
  value       = aws_internet_gateway.main.id
}

output "nat_gateway_ids" {
  description = "NAT Gateway ID 목록"
  value       = aws_nat_gateway.main[*].id
}

output "availability_zones" {
  description = "사용 중인 가용 영역 목록"
  value       = var.azs
}
