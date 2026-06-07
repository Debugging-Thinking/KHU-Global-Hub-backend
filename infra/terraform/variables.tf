variable "region" {
  description = "AWS 리전"
  type        = string
  default     = "ap-northeast-2"
}

variable "project" {
  description = "리소스 태그/이름 접두사"
  type        = string
  default     = "khu-global-hub"
}

variable "ec2_ami" {
  description = "EC2 AMI ID (현재 운영 인스턴스의 AMI로 교체 — `aws ec2 describe-instances`로 확인)"
  type        = string
}

variable "ec2_key_name" {
  description = "EC2 SSH 키페어 이름"
  type        = string
}

variable "s3_bucket_name" {
  description = "이미지 업로드 S3 버킷 이름 (현재 운영 버킷명)"
  type        = string
}

variable "db_username" {
  description = "RDS 마스터 사용자 — 실제 값은 terraform.tfvars(gitignore) 또는 TF_VAR_db_username 환경변수로"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "RDS 마스터 비밀번호 — 절대 코드/깃에 넣지 말 것"
  type        = string
  sensitive   = true
}

variable "my_ip_cidr" {
  description = "SSH(22) 허용 CIDR. 운영은 본인 IP/32 로 좁힐 것"
  type        = string
  default     = "0.0.0.0/0"
}
