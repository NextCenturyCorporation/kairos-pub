
data "aws_s3_bucket" "visualization" {
    bucket = local.s3_bucket
}

data "aws_iam_policy_document" "allow_cloudfront" {
  statement {

    sid = "AllowCloudFrontServicePrincipal"
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    actions = [
      "s3:GetObject"
    ]

    resources = [
      "${data.aws_s3_bucket.visualization.arn}/*",
    ]

    condition {
        test     = "StringLike"
        variable = "AWS:SourceArn"

        values = [aws_cloudfront_distribution.visualization.arn]
        }
    }
}

resource "aws_s3_bucket_policy" "allow_cloudfront" {
  bucket = data.aws_s3_bucket.visualization.id
  policy = data.aws_iam_policy_document.allow_cloudfront.json
}