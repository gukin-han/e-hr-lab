# Terraform Infrastructure

e-hr-lab AWS 인프라를 관리하는 Terraform 코드입니다.

## 디렉토리 구조

```
terraform/
├── environments/     # 환경별 설정
│   └── dev/          # 개발 환경
├── modules/          # 재사용 가능한 모듈
└── README.md
```

## 사전 요구사항

- Terraform >= 1.0.0
- AWS CLI 설정 완료 (`aws configure`)

## 사용법

```bash
# 개발 환경으로 이동
cd environments/dev

# 초기화
terraform init

# 변경사항 확인
terraform plan

# 적용
terraform apply

# 삭제
terraform destroy
```

## 환경별 변수

`terraform.tfvars` 파일을 생성하여 환경별 변수를 설정합니다.
이 파일은 `.gitignore`에 포함되어 있으므로 커밋되지 않습니다.

```hcl
# terraform.tfvars.example
project_name = "e-hr-lab"
environment  = "dev"
aws_region   = "ap-northeast-2"
```

## 주의사항

- `terraform.tfstate` 파일은 절대 커밋하지 마세요
- 프로덕션 환경에서는 S3 + DynamoDB 백엔드를 사용하세요
- `terraform apply` 전에 항상 `terraform plan`으로 변경사항을 확인하세요
