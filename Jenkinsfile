node {
    def image

    stage('Clone Repository') {
        checkout scm
    }

    stage('Maven Build') {
        docker.image('maven:3-alpine').inside('-v /root/.m2:/root/.m2') {
            stage('Build') {
                sh 'mvn -B clean install'
            }
        }
    }

    stage('Build Docker Image') {
        /* This builds the actual image; synonymous to
         * docker build on the command line */

        image = docker.build("docker.stammgruppe.eu/bass:${env.BUILD_NUMBER}")
    }

    stage('Push Docker Image') {
        // Make sure we are logged in to our repository
        withCredentials([UsernamePasswordMultiBinding(credentialsId: 'docker-stammgruppe', usernameVariable: 'USER', passwordVariable: 'PW')]) {
            sh 'docker login -u $USER -p $PW'
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