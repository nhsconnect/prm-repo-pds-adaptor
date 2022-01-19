environment    = "dev"
component_name = "pds-adaptor"
dns_name       = "pds-adaptor"
repo_name      = "prm-deductions-pds-adaptor"

task_cpu    = 2048
task_memory = 4096
port        = 8080

service_desired_count = "1"

alb_deregistration_delay = 15

grant_access_through_vpn = true
