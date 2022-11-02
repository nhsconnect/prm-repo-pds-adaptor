resource "aws_lb" "nlb" {
  name               = "${var.environment}-${var.component_name}-nlb"
  internal           = true
  load_balancer_type = "network"
  subnets            = split(",", data.aws_ssm_parameter.deductions_private_private_subnets.value)
  enable_deletion_protection = true

  tags = {
    Name = "${var.environment}-${var.component_name}-nlb"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_lb_target_group" "pds-nlb-target-group" {
  name        = "${var.environment}-${var.component_name}-nlb-tg"
  target_type = "alb"
  port        = 443
  protocol    = "TCP"
  vpc_id      = data.aws_ssm_parameter.deductions_private_vpc_id.value

  tags = {
    Name = "${var.environment}-${var.component_name}-nlb-target-group"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_lb_target_group_attachment" "pds-alb-target-group-attachment" {
  target_group_arn = aws_lb_target_group.pds-nlb-target-group.arn
  target_id        = aws_alb.alb-internal.id
  port             = 443
}

resource "aws_lb_listener" "pds-nlb-listener" {
  load_balancer_arn = aws_lb.nlb.arn
  port              = "443"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.pds-nlb-target-group.arn
  }

  tags = {
    Name = "${var.environment}-${var.component_name}-nlb-listener"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_vpc_endpoint_service" "pds-service-endpoint" {
  acceptance_required        = false
  network_load_balancer_arns = [aws_lb.nlb.arn]
  private_dns_name = "prs-endpoint.${var.dns_name}.${var.environment}.non-prod.patient-deductions.nhs.uk"

  tags = {
    Name = "${var.environment}-${var.component_name}-prs-service-endpoint"
    CreatedBy   = var.repo_name
    Environment = var.environment
  }
}

resource "aws_route53_record" "pds-service-domain-verification-record" {
  zone_id = data.aws_ssm_parameter.environment_private_zone_id.value
  name    = "prs-endpoint.${var.dns_name}"
  type    = "TXT"
  ttl     = 60
  records = [aws_vpc_endpoint_service.pds-service-endpoint.private_dns_name_configuration[0].value]
}

resource "aws_vpc_endpoint_service_allowed_principal" "allow_paperless_record_service_access" {
  vpc_endpoint_service_id = aws_vpc_endpoint_service.pds-service-endpoint.id
  principal_arn           = "arn:aws:iam::${data.aws_ssm_parameter.paperless-record-service-aws-account-id.value}:root"
}