terraform {

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.54.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "3.9.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

# 3 random digits appended to bucket names, generated once and stable across applies
resource "random_integer" "suffix" {
  min = 100
  max = 999
}

locals {
  suffix = tostring(414)

  landing_zone_bucket_name      = "${var.project_name}${local.suffix}-landing-zone"
  data_warehouse_bucket_name    = "${var.project_name}${local.suffix}-data-warehouse"
  data_quality_bucket_name      = "${var.project_name}${local.suffix}-data-quality"
  artifact_registry_bucket_name = "${var.project_name}${local.suffix}-artifact-registry"

  common_tags = {
    Project   = var.project_name
    ManagedBy = "terraform"
  }
}
