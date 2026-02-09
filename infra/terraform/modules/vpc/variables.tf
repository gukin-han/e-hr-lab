# =============================================================================
# VPC Module - Input Variables
# =============================================================================

variable "project_name" {
  description = "프로젝트 이름 (리소스 명명에 사용)"
  type        = string
}

variable "environment" {
  description = "배포 환경 (dev, staging, prod)"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "azs" {
  description = "사용할 가용 영역 목록"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

variable "public_subnet_cidrs" {
  description = "Public Subnet CIDR 목록 (AZ 개수와 동일해야 함)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "Private Subnet CIDR 목록 (AZ 개수와 동일해야 함)"
  type        = list(string)
  default     = ["10.0.11.0/24", "10.0.12.0/24"]
}

variable "enable_nat_gateway" {
  description = "NAT Gateway 생성 여부"
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "단일 NAT Gateway 사용 여부 (비용 절감, 가용성 감소)"
  type        = bool
  default     = true  # 개발 환경에서는 비용 절감을 위해 true
}

variable "tags" {
  description = "추가 태그"
  type        = map(string)
  default     = {}
}
