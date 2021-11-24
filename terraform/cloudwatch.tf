resource "aws_cloudwatch_log_group" "log_group" {
  name = "/nhs/deductions/${var.environment}-${data.aws_caller_identity.current.account_id}/${var.component_name}"

  tags = {
    Environment = var.environment
    CreatedBy= var.repo_name
  }
}

resource "aws_cloudwatch_log_metric_filter" "log_metric_filter" {
  name           = "${var.environment}-${var.component_name}-error-logs"
  pattern        = "{ $.level = \"ERROR\" }"
  log_group_name = aws_cloudwatch_log_group.log_group.name

  metric_transformation {
    name          = "ErrorCountInLogs"
    namespace     = "PdsAdaptor"
    value         = 1
    default_value = 0
  }
}

resource "aws_cloudwatch_metric_alarm" "error_log_alarm" {
  alarm_name                = "${var.environment}-${var.component_name}-error-logs"
  comparison_operator       = "GreaterThanThreshold"
  evaluation_periods        = "1"
  period                    = "60"
  metric_name               = aws_cloudwatch_log_metric_filter.log_metric_filter.metric_transformation[0].name
  namespace                 = aws_cloudwatch_log_metric_filter.log_metric_filter.metric_transformation[0].namespace
  statistic                 = "Sum"
  threshold                 = "0"
  alarm_description         = "This alarm monitors errors logs in ${var.component_name}"
  treat_missing_data        = "missing"
}