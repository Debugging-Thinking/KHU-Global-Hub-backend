# 운영 EC2 (t3.micro, ap-northeast-2) + 고정 IP(Elastic IP).
# 현재 운영 인스턴스를 import 한 뒤 ami/key_name 을 실제 값으로 맞추고 plan="no changes" 확인.
resource "aws_instance" "app" {
  ami                    = var.ec2_ami
  instance_type          = "t3.micro"
  key_name               = var.ec2_key_name
  vpc_security_group_ids = [aws_security_group.web.id]

  tags = {
    Name    = "${var.project}-app"
    Project = var.project
  }

  lifecycle {
    # import 전 실수로 apply 해도 인스턴스 교체/삭제 방지
    prevent_destroy = true
    # AMI는 시간이 지나면 deprecated 되므로 재생성 트리거 방지
    ignore_changes = [ami]
  }
}

# Elastic IP — 재부팅해도 IP 고정 (도메인 연결 전 필수).
# 아직 EIP를 안 붙였다면, import 대신 이 리소스만 실제로 apply 해서 새로 할당해도 됨.
resource "aws_eip" "app" {
  instance = aws_instance.app.id
  domain   = "vpc"

  tags = {
    Project = var.project
  }
}

output "public_ip" {
  description = "EC2 고정 공인 IP (DuckDNS A레코드에 넣을 값)"
  value       = aws_eip.app.public_ip
}
