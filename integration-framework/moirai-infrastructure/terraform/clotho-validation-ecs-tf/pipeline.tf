# resource "aws_codepipeline" "codepipeline" {
#   name     = "tf-clotho-pipeline"
#   role_arn = aws_iam_role.pipeline.arn

#   # artifact_store {
#   #   location = "130602597458.dkr.ecr.us-east-1.amazonaws.com/genesis" # Hard coded because theres no output of this
#   #   type     = "S3"
#   # }

#   stage {
#     name = "Source"

#     action {
#       name             = "Source"
#       category         = "Source"
#       owner            = "AWS"
#       provider         = "ECR"
#       version          = "1"
#       output_artifacts = ["sourceArtifact"]

#       configuration = {
#         RepositoryName = "genesis"
#         ImageTag       = "clotho-latest"
#       }
#     }
#   }

#   stage {
#     name = "Deploy"

#     action {
#       name             = "Deploy"
#       category         = "Deploy"
#       owner            = "AWS"
#       provider         = "CloudFormation"
#       input_artifacts  = ["sourceArtifact"]
#       version          = "1"

#        configuration = {
#         ApplicationName = "${var.app}-service-deploy"
#         DeploymentGroupName = "${var.app}-service-deploy-group"
#         TaskDefinitionTemplateArtifact = "BuildArtifact"
#         TaskDefinitionTemplatePath = "taskdef.json"
#         AppSpecTemplateArtifact = "BuildArtifact"
#         AppSpecTemplatePath = "appspec.yml"
#       }
#     }
#   }
# }