locals {
  image_uri = var.container_image != "" ? var.container_image : "${aws_ecr_repository.app.repository_url}:latest"

  alb_default_origin = "http://${aws_lb.main.dns_name}"
  cors_origins = var.cors_allowed_origins != "" ? var.cors_allowed_origins : local.alb_default_origin

  db_url = "jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${var.db_name}"
}

resource "aws_ecs_cluster" "main" {
  name = "${local.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name       = aws_ecs_cluster.main.name
  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
    base              = 1
  }
}

resource "aws_ecs_task_definition" "app" {
  family                   = "${local.name_prefix}-app"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "app"
      image     = local.image_uri
      essential = true

      portMappings = [
        {
          containerPort = var.container_port
          hostPort      = var.container_port
          protocol      = "tcp"
        }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "prod" },
        { name = "PORT", value = tostring(var.container_port) },
        { name = "DB_URL", value = local.db_url },
        { name = "DB_USERNAME", value = var.db_username },
        { name = "ADMIN_USERNAME", value = var.admin_username },
        { name = "CORS_ALLOWED_ORIGINS", value = local.cors_origins },
        { name = "JAVA_TOOL_OPTIONS", value = "-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" }
      ]

      secrets = [
        { name = "DB_PASSWORD", valueFrom = "${aws_secretsmanager_secret.app.arn}:db_password::" },
        { name = "JWT_SECRET", valueFrom = "${aws_secretsmanager_secret.app.arn}:jwt_secret::" },
        { name = "ADMIN_PASSWORD", valueFrom = "${aws_secretsmanager_secret.app.arn}:admin_password::" }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.app.name
          awslogs-region        = var.region
          awslogs-stream-prefix = "app"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:${var.container_port}/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])
}

resource "aws_ecs_service" "app" {
  name            = "${local.name_prefix}-svc"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  # Allow CI/CD to update the image without Terraform fighting it.
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name   = "app"
    container_port   = var.container_port
  }

  health_check_grace_period_seconds = 90
  deployment_minimum_healthy_percent = 100
  deployment_maximum_percent         = 200

  depends_on = [
    aws_lb_listener.http,
    aws_db_instance.main
  ]
}
