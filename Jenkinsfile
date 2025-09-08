pipeline {
    agent any
    triggers {
        githubPush()
    }
    environment {
        REGISTRY = "junga970"
        NAMESPACE = "momnect"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Push Docker Images') {
            steps {
                script {
                    def services = [
                        "chat-service", "discovery-service", "file-service",
                        "gateway-service", "post-service", "product-service",
                        "review-service", "user-service", "websocket-service"
                    ]

                    withCredentials([usernamePassword(credentialsId: 'DOCKERHUB_PASSWORD', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        bat "echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin"

                        for (service in services) {
                            dir(service) {
                                bat "echo Building Docker image for ${service}"
                                bat "docker build -t ${REGISTRY}/${service}:latest ."
                                bat "docker push ${REGISTRY}/${service}:latest"
                            }
                        }
                    }
                }
            }
        }

        stage('Rolling Update Deployments') {
            steps {
                script {
                    def services = [
                        "chat-service", "discovery-service", "file-service",
                        "gateway-service", "post-service", "product-service",
                        "review-service", "user-service", "websocket-service"
                    ]

                    withCredentials([file(credentialsId: 'KUBECONFIG_EC2', variable: 'KUBECONFIG')]) {
                        for (service in services) {
                            //bat "kubectl --kubeconfig=%KUBECONFIG% set image deployment/${service} ${service}=${REGISTRY}/${service}:latest -n ${NAMESPACE}"
                            bat "kubectl --kubeconfig=%KUBECONFIG% rollout restart deployment/${service} -n ${NAMESPACE}"
                        }
                    }
                }
            }
        }
    }
}
