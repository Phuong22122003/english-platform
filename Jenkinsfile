pipeline {
    agent any

    environment {
        // Image name for each service
        API_GATEWAY_IMAGE = 'phuongnbp/api-gateway:latest'
        USER_SERVICE_IMAGE = 'phuongnbp/user-service:latest'
        CONTENT_SERVICE_IMAGE = 'phuongnbp/content-service:latest'
        LEARNING_SERVICE_IMAGE = 'phuongnbp/learning-service:latest'
        COMPOSE_FILE = 'docker-compose.yml'
    }

    stages {
        // stage('Checkout Source Code') {
        //     steps {
        //         echo '🔹 Checking out source code...'
        //         git branch: 'main', url: 'https://github.com/Phuong22122003/english-web.git'
        //     }
        // }
        stage('Build Core Module') {
            steps {
                script {
                    echo '⚙️ Building core module (shared library)...'
                    sh '''
                        cd core
                        mvn clean install -DskipTests
                    '''
                }
            }
        }

        stage('Build Other Services') {
            steps {
                script {
                    echo '🏗️ Building other Spring Boot microservices...'
                    sh '''
                        mvn -f user-service/pom.xml clean package -DskipTests
                        mvn -f content-service/pom.xml clean package -DskipTests
                        mvn -f learning-service/pom.xml clean package -DskipTests
                    '''
                }
            }
        }

        stage('Clean Old Docker Images') {
            steps {
                script {
                    echo '🧹 Removing old Docker images...'
                    sh '''
                        docker compose down --remove-orphans || true
                        docker rmi -f ${API_GATEWAY_IMAGE} || true
                        docker rmi -f ${USER_SERVICE_IMAGE} || true
                        docker rmi -f ${CONTENT_SERVICE_IMAGE} || true
                        docker rmi -f ${LEARNING_SERVICE_IMAGE} || true
                    '''
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    echo '🏗️ Building Docker images for each service...'
                    sh '''
                        cd core && mvn clean package
                    '''
                    sh '''
                        docker build -t ${API_GATEWAY_IMAGE} -f api-gateway/Dockerfile api-gateway
                        docker build -t ${USER_SERVICE_IMAGE} -f user-service/Dockerfile user-service
                        docker build -t ${CONTENT_SERVICE_IMAGE} -f content-service/Dockerfile content-service
                        docker build -t ${LEARNING_SERVICE_IMAGE} -f learning-service/Dockerfile learning-service
                    '''
                }
            }
        }

        stage('Run Docker Compose') {
            steps {
                script {
                    echo 'Starting all services using docker-compose...'
                    sh '''
                        docker-compose down --remove-orphans
                        docker-compose up -d --build
                    '''
                }
            }
        }

        stage('Verify Deployment') {
            steps {
                script {
                    echo 'Checking running containers...'
                    sh 'docker ps'
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment successful! All microservices are up and running.'
        }
        failure {
            echo 'Build or deployment failed. Check Jenkins logs for details.'
        }
    }
}
