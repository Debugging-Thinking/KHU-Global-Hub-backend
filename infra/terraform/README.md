# Terraform — KHU Global Hub 인프라 (코드 기술 단계)

> **현재 상태: "코드만 작성" 단계. 아직 Terraform이 운영을 관리하지 않는다.**
> 이 디렉터리는 지금 돌고 있는 운영 인프라(EC2 / RDS / S3 / 보안그룹 / EIP)를
> **코드로 기술**해둔 것이다. 문서이자, 나중에 `import`로 실제 연결할 출발점.

## ⛔ 가장 중요한 규칙

**빈 상태(state)에서 `terraform apply` 절대 금지.**
지금 apply 하면 Terraform은 "이 리소스들이 없다"고 보고 **새로** 만들려 한다 →
- 이름 충돌로 실패하거나,
- 운이 나쁘면 **중복 인프라가 생기고 과금**된다.

실데이터가 든 RDS·S3에는 `prevent_destroy` + `deletion_protection`을 박아놨지만,
그건 최후의 안전장치일 뿐이다. **순서는 항상 `import` → `plan`(no changes 확인) → 그 다음에만 `apply`.**

## 파일 구성

| 파일 | 내용 |
|------|------|
| `provider.tf` | AWS provider(ap-northeast-2), state 백엔드(주석, 추후 S3) |
| `variables.tf` | 변수 정의 (민감값은 tfvars/환경변수로) |
| `network.tf` | 보안그룹(80/443 공개, 22 본인IP, 8080 비공개) |
| `ec2.tf` | EC2 t3.micro + Elastic IP + `public_ip` output |
| `rds.tf` | RDS PostgreSQL 17 (삭제방지 이중) |
| `s3.tf` | 이미지 버킷 |
| `terraform.tfvars.example` | 변수값 템플릿 → `terraform.tfvars`로 복사(gitignore) |

## 나중에 "실제로 연결"하려면 (import 절차)

> 선행: `terraform`·`aws` CLI 설치 + `aws configure`(IAM 키). 운영 변경 전 **RDS 스냅샷**.

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars   # 실제 값 채우기 (깃에 안 올라감)
terraform init

# 실제 리소스 ID는 콘솔이나 aws CLI로 확인 후 import:
terraform import aws_instance.app           i-0xxxxxxxxxxxxxxxx
terraform import aws_eip.app                eipalloc-0xxxxxxxxxxxxxxxx
terraform import aws_security_group.web     sg-0xxxxxxxxxxxxxxxx
terraform import aws_db_instance.postgres   khu-global-hub-db        # DB identifier
terraform import aws_s3_bucket.images       your-image-bucket-name

# 핵심 검증 — 코드가 실물과 일치하면 "No changes"가 떠야 한다.
terraform plan
```

`plan`에서 변경(특히 **destroy/replace**)이 뜨면 **apply 하지 말고** 코드를 실물에 맞춰 수정한다.
`No changes`가 될 때까지 맞춘 뒤에야 Terraform이 신뢰할 수 있는 상태가 된다.

## 리소스 ID 빠르게 찾기

```bash
aws ec2 describe-instances --filters "Name=instance-state-name,Values=running" \
  --query "Reservations[].Instances[].{id:InstanceId,ami:ImageId,key:KeyName}" --output table
aws ec2 describe-addresses --query "Addresses[].{ip:PublicIp,alloc:AllocationId}" --output table
aws rds describe-db-instances --query "DBInstances[].DBInstanceIdentifier" --output table
aws s3 ls
```

## 팀 공유 전 (선택)

여러 명이 쓰면 로컬 state가 충돌한다. `provider.tf`의 주석 처리된 S3 백엔드를 켜고
state 전용 버킷 + DynamoDB 잠금 테이블을 만들어 원격 state로 전환할 것.
