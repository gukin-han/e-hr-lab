# AWS + Terraform 학습 로드맵

> 이 문서는 e-hr-lab 프로젝트를 AWS에 배포하기 위한 학습 로드맵입니다.
> 단순 배포가 아닌, **IaC(Infrastructure as Code)** 원칙에 따라 Terraform으로 인프라를 코드화하는 것이 목표입니다.

---

## 1. 개요

### 목표
- Spring Boot 애플리케이션을 **AWS ECS Fargate**에 컨테이너로 배포
- 모든 인프라를 **Terraform**으로 관리 (수동 클릭 없이 재현 가능한 환경)
- 비용 효율적인 학습 환경 구성 (Free Tier + Spot 활용)

### 학습 방향
1. **점진적 복잡도 증가**: VPC → ECS → RDS 순서로 단계별 학습
2. **실습 중심**: 각 Phase마다 `terraform apply`로 실제 리소스 생성
3. **비용 인식**: 항상 `terraform destroy`로 정리하는 습관

---

## 2. Phase 1: 기초 환경 설정

### 2.1 AWS 계정 준비
- [ ] AWS 계정 생성 (Free Tier 활용)
- [ ] IAM 사용자 생성 (AdministratorAccess, 루트 계정 사용 금지)
- [ ] MFA 활성화

### 2.2 로컬 도구 설치
```bash
# AWS CLI 설치
brew install awscli

# Terraform 설치
brew install terraform

# 버전 확인
aws --version
terraform version
```

### 2.3 AWS CLI 설정
```bash
aws configure
# AWS Access Key ID: [IAM에서 생성한 키]
# AWS Secret Access Key: [비밀 키]
# Default region name: ap-northeast-2
# Default output format: json

# 연결 확인
aws sts get-caller-identity
```

### 2.4 Terraform 기초 실습
```hcl
# main.tf - 가장 간단한 예제
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-northeast-2"
}

# S3 버킷 생성 (Terraform 상태 저장용으로도 활용 가능)
resource "aws_s3_bucket" "test" {
  bucket = "e-hr-lab-terraform-test-${random_id.suffix.hex}"
}

resource "random_id" "suffix" {
  byte_length = 4
}
```

```bash
terraform init    # 프로바이더 다운로드
terraform plan    # 변경 사항 미리보기
terraform apply   # 실제 생성
terraform destroy # 정리
```

---

## 3. Phase 2: 네트워크 구성 (VPC)

### 학습 목표
- AWS 네트워크의 기본 구조 이해
- Public/Private Subnet 분리의 의미 파악

### 구성 요소
```
VPC (10.0.0.0/16)
├── Public Subnet (10.0.1.0/24)  - ALB 배치
├── Private Subnet (10.0.2.0/24) - ECS Tasks 배치
├── Internet Gateway             - 외부 통신
├── NAT Gateway                  - Private → 외부 (비용 주의!)
└── Security Groups              - 방화벽 규칙
```

### 핵심 리소스
- `aws_vpc`: 가상 네트워크
- `aws_subnet`: 서브넷 (AZ별로 최소 2개 권장)
- `aws_internet_gateway`: 인터넷 연결
- `aws_nat_gateway`: Private 서브넷 외부 통신 (시간당 과금)
- `aws_security_group`: 인바운드/아웃바운드 규칙

### 비용 팁
- **NAT Gateway는 비쌈** ($0.045/시간 + 데이터 처리 비용)
- 학습 중에는 Public Subnet만 사용하거나, NAT Instance(t3.micro) 대안 고려

---

## 4. Phase 3: 컨테이너 배포 (ECS Fargate)

### 학습 목표
- Docker 이미지를 ECR에 푸시
- ECS Fargate로 서버리스 컨테이너 운영
- ALB로 트래픽 분산

### 4.1 ECR (Elastic Container Registry)
```bash
# ECR 리포지토리 생성 후 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com

# 이미지 빌드 & 푸시
docker build -t e-hr-lab .
docker tag e-hr-lab:latest <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/e-hr-lab:latest
docker push <account-id>.dkr.ecr.ap-northeast-2.amazonaws.com/e-hr-lab:latest
```

### 4.2 ECS 구성 요소
```
ECS Cluster
└── Service (Desired Count: 1)
    └── Task Definition
        ├── Container: e-hr-lab
        ├── CPU: 256 (0.25 vCPU)
        ├── Memory: 512 MB
        └── Port: 9090
```

### 4.3 ALB (Application Load Balancer)
- Target Group: ECS 서비스와 연결
- Listener: 80 → 9090 포워딩
- Health Check: `/actuator/health`

### Terraform 리소스
- `aws_ecr_repository`
- `aws_ecs_cluster`
- `aws_ecs_task_definition`
- `aws_ecs_service`
- `aws_lb`, `aws_lb_target_group`, `aws_lb_listener`

### 비용 팁
- **Fargate Spot**: 최대 70% 할인 (중단 가능성 있음, 학습용으로 적합)
- 최소 스펙 사용: 0.25 vCPU / 512MB

---

## 5. Phase 4: 데이터베이스 선택

### 옵션 비교

| 구분 | H2 (In-Memory) | RDS PostgreSQL |
|------|----------------|----------------|
| 비용 | 무료 | Free Tier: t3.micro 12개월 |
| 데이터 지속성 | 재시작 시 초기화 | 영구 저장 |
| 설정 복잡도 | 없음 | VPC, Security Group 연동 필요 |
| 학습 가치 | 낮음 | 높음 (실무와 동일) |

### 권장 시나리오
- **Phase 3까지**: H2로 빠르게 배포 확인
- **Phase 4**: RDS 추가하여 실제 DB 연동 경험

### RDS 구성 시 고려사항
- Private Subnet에 배치
- Security Group: ECS Task에서만 접근 허용 (포트 5432)
- 파라미터 그룹: 타임존, 문자셋 설정

---

## 6. Phase 5: 테스트 및 정리

### 6.1 부하 테스트 (k6)
```bash
# ALB DNS를 대상으로 테스트
k6 run -e BASE_URL=http://<alb-dns> k6/results/io-test.js
```

### 6.2 모니터링 (CloudWatch)
- ECS 메트릭: CPU, 메모리 사용률
- ALB 메트릭: 요청 수, 응답 시간, 5xx 에러
- Container Insights 활성화 (추가 비용 발생)

### 6.3 정리
```bash
# 모든 리소스 삭제 (비용 방지)
terraform destroy

# 삭제 확인
aws ecs list-clusters
aws rds describe-db-instances
```

---

## 7. 디렉토리 구조 제안

```
infra/
└── terraform/
    ├── environments/
    │   ├── dev/
    │   │   ├── main.tf
    │   │   ├── variables.tf
    │   │   └── terraform.tfvars
    │   └── prod/           # 향후 확장
    │
    ├── modules/
    │   ├── vpc/
    │   │   ├── main.tf
    │   │   ├── variables.tf
    │   │   └── outputs.tf
    │   ├── ecs/
    │   ├── alb/
    │   └── rds/
    │
    └── README.md           # Terraform 사용법
```

### 모듈화 전략
- **modules/**: 재사용 가능한 인프라 컴포넌트
- **environments/**: 환경별 설정값 (dev, staging, prod)
- 변수화: 하드코딩 대신 `var.xxx` 사용

---

## 8. 비용 관리 팁

### 8.1 AWS Budget 설정
```bash
# 월 $10 초과 시 알림
aws budgets create-budget \
  --account-id <account-id> \
  --budget file://budget.json \
  --notifications-with-subscribers file://notifications.json
```

### 8.2 비용 최적화 체크리스트
- [ ] **NAT Gateway**: 사용하지 않을 때 삭제 ($32/월)
- [ ] **ECS**: Fargate Spot 사용 (최대 70% 절감)
- [ ] **RDS**: 사용하지 않을 때 중지 (7일 후 자동 시작 주의)
- [ ] **ALB**: 요청 없어도 시간당 과금 ($16/월)
- [ ] **ECR**: 오래된 이미지 정리 (Lifecycle Policy)

### 8.3 Free Tier 한도 (12개월)
- EC2: t2.micro 750시간/월
- RDS: db.t2.micro 750시간/월
- S3: 5GB
- Lambda: 100만 요청/월

---

## 9. 참고 자료

### 공식 문서
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS ECS 개발자 가이드](https://docs.aws.amazon.com/ecs/)
- [Terraform Best Practices](https://www.terraform-best-practices.com/)

### 추천 학습 순서
1. Terraform 공식 튜토리얼 (Learn HashiCorp)
2. AWS Free Tier로 직접 실습
3. 본 프로젝트에 단계별 적용

---

**[Next Step]**
Phase 1부터 시작하여, 각 단계를 완료할 때마다 이 문서에 체크표시를 업데이트하고 `90_Engineering_Log/`에 학습 기록을 남긴다.
