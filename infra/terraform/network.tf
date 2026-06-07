# 기존 default VPC 사용. import 단계에서 실제 VPC를 쓰면 data 블록은 그대로 두고
# 각 리소스의 vpc_id 만 실제 값과 일치하는지 plan 으로 확인.
data "aws_vpc" "default" {
  default = true
}

# EC2 보안 그룹 — HTTP/HTTPS는 전체 공개, SSH는 본인 IP, 8080은 외부 미개방(Nginx가 localhost 프록시)
resource "aws_security_group" "web" {
  name        = "${var.project}-web"
  description = "HTTP/HTTPS/SSH for KHU Global Hub EC2"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.my_ip_cidr]
  }

  egress {
    description = "all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project = var.project
  }
}
