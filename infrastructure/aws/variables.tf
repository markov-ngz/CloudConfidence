variable "project_name" {
  description = "Project name used as a prefix for resource naming"
  type        = string
}

variable "aws_region" {
  description = "AWS region to deploy resources into"
  type        = string
  default     = "eu-north-1"
}