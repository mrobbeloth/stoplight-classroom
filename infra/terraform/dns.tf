# ----- Route 53 + ACM (only created when domain_name is set) -----

variable "domain_name" {
  description = "Root domain name (e.g. stoplightonline.com). If empty, DNS/TLS resources are not created."
  type        = string
  default     = ""
}

variable "create_www_redirect" {
  description = "Whether to create a www.domain alias pointing to the same ALB."
  type        = bool
  default     = true
}

locals {
  create_dns = var.domain_name != ""
  # Subject Alternative Names: bare domain + www
  cert_sans  = local.create_dns && var.create_www_redirect ? ["www.${var.domain_name}"] : []
}

# Look up the existing hosted zone (Route 53 creates one when you register a domain).
data "aws_route53_zone" "main" {
  count = local.create_dns ? 1 : 0
  name  = var.domain_name
}

# ----- ACM certificate -----

resource "aws_acm_certificate" "main" {
  count             = local.create_dns ? 1 : 0
  domain_name       = var.domain_name
  subject_alternative_names = local.cert_sans
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${local.name_prefix}-cert"
  }
}

# Create DNS records for certificate validation.
resource "aws_route53_record" "cert_validation" {
  for_each = local.create_dns ? {
    for dvo in aws_acm_certificate.main[0].domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  } : {}

  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = each.value.name
  type    = each.value.type
  records = [each.value.record]
  ttl     = 60

  allow_overwrite = true
}

# Wait for the certificate to be validated.
resource "aws_acm_certificate_validation" "main" {
  count                   = local.create_dns ? 1 : 0
  certificate_arn         = aws_acm_certificate.main[0].arn
  validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

# ----- DNS records pointing at the ALB -----

resource "aws_route53_record" "apex_a" {
  count   = local.create_dns ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = var.domain_name
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "apex_aaaa" {
  count   = local.create_dns ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = var.domain_name
  type    = "AAAA"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "www_a" {
  count   = local.create_dns && var.create_www_redirect ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = "www.${var.domain_name}"
  type    = "A"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

resource "aws_route53_record" "www_aaaa" {
  count   = local.create_dns && var.create_www_redirect ? 1 : 0
  zone_id = data.aws_route53_zone.main[0].zone_id
  name    = "www.${var.domain_name}"
  type    = "AAAA"

  alias {
    name                   = aws_lb.main.dns_name
    zone_id                = aws_lb.main.zone_id
    evaluate_target_health = true
  }
}

# ----- Wire the validated cert into the ALB -----

output "certificate_arn" {
  description = "ACM certificate ARN (for reference / debugging)."
  value       = local.create_dns ? aws_acm_certificate.main[0].arn : ""
}

output "app_url" {
  description = "The public URL for the application."
  value       = local.create_dns ? "https://${var.domain_name}" : "http://${aws_lb.main.dns_name}"
}
