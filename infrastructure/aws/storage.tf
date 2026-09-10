
resource "aws_s3_bucket" "landing_zone" {
  bucket = local.landing_zone_bucket_name

  tags = merge(local.common_tags, {
    Name = local.landing_zone_bucket_name
    Role = "landing-zone"
  })
}

resource "aws_s3_bucket" "data_warehouse" {
  bucket = local.data_warehouse_bucket_name

  tags = merge(local.common_tags, {
    Name = local.data_warehouse_bucket_name
    Role = "data-warehouse"
  })
}

resource "aws_s3_bucket" "data_quality" {
  bucket = local.data_quality_bucket_name

  tags = merge(local.common_tags, {
    Name = local.data_quality_bucket_name
    Role = "data-quality"
  })
}

resource "aws_s3_bucket" "artifact_registry" {
  bucket = local.artifact_registry_bucket_name

  tags = merge(local.common_tags, {
    Name = local.artifact_registry_bucket_name
    Role = "artifact-registry"
  })
}