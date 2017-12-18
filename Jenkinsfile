node {
    if (env.BRANCH_NAME == 'master' || env.BRANCH_NAME == 'dev') {
        def image

        stage('Clone Repository') {
            checkout scm
        }

        stage('Maven Build') {
            docker.image('maven:3-alpine').inside('-v /root/.m2:/root/.m2') {
                sh 'mvn -B clean install'
            }

            archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
        }

        stage('Build Docker Image') {
            /* This builds the actual image; synonymous to
             * docker build on the command line */

            if (env.BRANCH_NAME == 'master') {
                image = docker.build("docker.stammgruppe.eu/bass:${env.BUILD_NUMBER}")
            } else if (env.BRANCH_NAME == 'dev') {
                image = docker.build("docker.stammgruppe.eu/bass-dev:${env.BUILD_NUMBER}")
            }
        }

        stage('Push Docker Image') {
            // Make sure we are logged in to our repository
            withCredentials([usernamePassword(credentialsId: 'docker-stammgruppe', usernameVariable: 'USER', passwordVariable: 'PW')]) {
                sh 'docker login -u $USER -p $PW docker.stammgruppe.eu'
            }

            /* Finally, we'll push the image with two tags:
             * First, the incremental build number from Jenkins
             * Second, the 'latest' tag.
             * Pushing multiple tags is cheap, as all the layers are reused. */
            docker.withRegistry('https://docker.stammgruppe.eu/v2/', 'docker-stammgruppe') {
                image.push("${env.BUILD_NUMBER}")
                image.push("latest")
            }
        }
    }
}