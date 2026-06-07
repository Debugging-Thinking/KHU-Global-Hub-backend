# 운영 RDS PostgreSQL 17. ⚠️ 실데이터 보유 — 절대 재생성/삭제 금지.
resource "aws_db_instance" "postgres" {
  identifier        = "${var.project}-db"
  engine            = "postgres"
  engine_version    = "17"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  db_name           = "khu_global_hub"
  username          = var.db_username
  password          = var.db_password

  skip_final_snapshot = false
  deletion_protection = true # AWS 레벨 삭제 방지

  lifecycle {
    prevent_destroy = true # Terraform 레벨 삭제 방지(이중 안전장치)
    ignore_changes = [
      password,       # 비번 회전은 콘솔/별도 관리
      engine_version, # 마이너 자동 업그레이드로 인한 drift 무시
    ]
  }

  tags = {
    Project = var.project
  }
}

output "db_endpoint" {
  description = "RDS 엔드포인트"
  value       = aws_db_instance.postgres.endpoint
  sensitive   = true
}
