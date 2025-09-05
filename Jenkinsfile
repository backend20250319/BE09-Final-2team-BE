pipeline {
    agent any
    triggers {
        githubPush()
    }
    environment {
        REGISTRY = "junga970"
        NAMESPACE = "momnect"
        PATH = "/usr/local/bin:/usr/bin:/bin"
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
                        sh "echo \"$DOCKER_PASS\" | docker login -u \"$DOCKER_USER\" --password-stdin"

                        for (service in services) {
                            dir(service) {
                                sh """
                                echo "🚀 Building Docker image for ${service}"
                                docker build -t ${REGISTRY}/${service}:dev-${env.BUILD_NUMBER} .
                                docker push ${REGISTRY}/${service}:dev-${env.BUILD_NUMBER}
                                """
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
                            sh """
                            kubectl --kubeconfig=$KUBECONFIG set image deployment/${service} ${service}=${REGISTRY}/${service}:dev-${env.BUILD_NUMBER} -n ${NAMESPACE}
                            """
                        }
                    }
                }
            }
        }
    }
}
