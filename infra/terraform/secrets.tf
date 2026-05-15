resource "random_password" "db_master" {
  length  = 32
  special = true
  # RDS master password disallows: / @ " and space
  override_special = "!#$%&*()-_=+[]{}<>:?"
}

resource "random_password" "jwt_secret" {
  length  = 64
  special = false
}

resource "random_password" "admin_password" {
  length           = 24
  special          = true
  override_special = "!#$%&*()-_=+"
}

# Single Secrets Manager entry holding all app secrets as JSON. Saves ~$0.80/mo
# vs three separate secrets. ECS pulls individual keys via the `:json-key::` syntax.
resource "aws_secretsmanager_secret" "app" {
  name                    = "${local.name_prefix}/app"
  description             = "Stoplight Classroom app secrets (DB password, JWT secret, admin password)"
  recovery_window_in_days = 7
}

resource "aws_secretsmanager_secret_version" "app" {
  secret_id = aws_secretsmanager_secret.app.id
  secret_string = jsonencode({
    db_password    = random_password.db_master.result
    jwt_secret     = random_password.jwt_secret.result
    admin_password = random_password.admin_password.result
  })
}
