pipeline {
    agent any

    stages {
        stage('Maven Build') {
            agent {
                docker {
                    image 'maven:3-alpine'
                    args  '-v /root/.m2:/root/.m2'
                }
            }

            steps {
                sh 'mvn -B clean install'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    def image = docker.build("docker.stammgruppe.eu/bass:${env.BUILD_NUMBER}")
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://docker.stammgruppe.eu', 'docker-stammgruppe') {
                        image.push("${env.BUILD_NUMBER}")
                        image.push("latest")
                    }
                }
            }
        }
    }
}