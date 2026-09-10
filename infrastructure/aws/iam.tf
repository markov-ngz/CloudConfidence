# User
resource "aws_iam_user" "service_user" {
  name = "${var.project_name}-svc-buckets-rw"
  tags = local.common_tags
}

# Role bucket rw

resource "aws_iam_role" "buckets_rw" {
  name               = "${var.project_name}-buckets-rw-role"
  assume_role_policy = data.aws_iam_policy_document.assume_role_trust.json
  tags               = local.common_tags

  max_session_duration = 3600
}

# Policy document to allow a user to assume  a role
data "aws_iam_policy_document" "assume_role_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "AWS"
      identifiers = [aws_iam_user.service_user.arn]
    }
  }
}

# Policy document existing to allow  a role to be assumed
data "aws_iam_policy_document" "allow_assume_role" {
  statement {
    effect    = "Allow"
    actions   = ["sts:AssumeRole"]
    resources = [aws_iam_role.buckets_rw.arn]
  }
}

#  Policy content for read and write to those buckets
data "aws_iam_policy_document" "buckets_rw" {
  statement {
    sid    = "ListBuckets"
    effect = "Allow"
    actions = [
      "s3:ListBucket",
      "s3:GetBucketLocation"
    ]
    resources = [
      aws_s3_bucket.landing_zone.arn,
      aws_s3_bucket.data_warehouse.arn,
      aws_s3_bucket.data_quality.arn,
      aws_s3_bucket.artifact_registry.arn,
    ]
  }

  statement {
    sid    = "ReadWriteObjects"
    effect = "Allow"
    actions = [
      "s3:ListObject",
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
      "s3:GetObjectVersion"
    ]
    resources = [
      "${aws_s3_bucket.landing_zone.arn}/*",
      "${aws_s3_bucket.data_warehouse.arn}/*",
      "${aws_s3_bucket.data_quality.arn}/*",
      "${aws_s3_bucket.artifact_registry.arn}/*",
    ]
  }
}

# Policy with the document
resource "aws_iam_policy" "buckets_rw" {
  name        = "${var.project_name}-buckets-rw-policy"
  description = "Read/write access to landing zone and data warehouse buckets"
  policy      = data.aws_iam_policy_document.buckets_rw.json
}

# Attach rw bucket policy  to the role
resource "aws_iam_role_policy_attachment" "buckets_rw" {
  role       = aws_iam_role.buckets_rw.name
  policy_arn = aws_iam_policy.buckets_rw.arn
}

# Optional : attach rw bucket policy to the user
resource "aws_iam_user_policy_attachment" "service_user_rw" {
  user       = aws_iam_user.service_user.name
  policy_arn = aws_iam_policy.buckets_rw.arn
}

# Allow the role rw bucket to be assumed
resource "aws_iam_policy" "allow_assume_role" {
  name   = "${var.project_name}-assume-buckets-rw-role"
  policy = data.aws_iam_policy_document.allow_assume_role.json
}

# Allow the user assume role
resource "aws_iam_user_policy_attachment" "service_user_assume_role" {
  user       = aws_iam_user.service_user.name
  policy_arn = aws_iam_policy.allow_assume_role.arn
}