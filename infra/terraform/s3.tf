# 이미지 업로드 버킷. ⚠️ 운영 이미지 보유 — 삭제 금지.
resource "aws_s3_bucket" "images" {
  bucket = var.s3_bucket_name

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Project = var.project
  }
}

# 퍼블릭 읽기(이미지 URL 직접 접근)를 쓰는 경우의 정책 — 실제 운영 설정과 맞춰 import.
# 현재 운영이 CloudFront/사인드URL 등 다른 방식이면 이 블록은 import 시 조정.
resource "aws_s3_bucket_public_access_block" "images" {
  bucket = aws_s3_bucket.images.id

  block_public_acls       = false
  block_public_policy      = false
  ignore_public_acls       = false
  restrict_public_buckets  = false
}
