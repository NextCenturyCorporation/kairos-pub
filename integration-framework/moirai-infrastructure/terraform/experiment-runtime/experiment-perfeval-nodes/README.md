This project is designed to be used with a seperate backend for each experiment.

To create experiment:
    terraform init -backend-config="key=experiment-perfeval-<<experiment name>>
    terraform apply -var="experiment=<<experiment name>>"

To destroy experiment:
    terraform init -backend-config="key=experiment-perfeval-<<experiment name>>
    terraform destroy -var="experiment=<<experiment name>>"