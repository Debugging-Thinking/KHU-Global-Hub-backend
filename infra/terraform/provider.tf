terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # 상태(state)는 현재 로컬 파일. 팀 공유/운영 import 단계에서는
  # 아래처럼 S3 + DynamoDB 잠금 백엔드로 전환 권장 (README 참고):
  #
  # backend "s3" {
  #   bucket         = "khu-global-hub-tfstate"
  #   key            = "prod/terraform.tfstate"
  #   region         = "ap-northeast-2"
  #   dynamodb_table = "khu-global-hub-tflock"
  #   encrypt        = true
  # }
}

provider "aws" {
  region = var.region
}
